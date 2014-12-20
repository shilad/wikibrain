package org.wikibrain.sr.factorized;

import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.InMemorySparseMatrix;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class StochasticNeighborhoodEmbedder {
    private static final Logger LOG = Logger.getLogger(StochasticNeighborhoodEmbedder.class.getCanonicalName());
    private Random random = new Random();
    private final int rank;
    private double vectors[][];

    private double threshold = 0.3;
    private Env env;

    private double initialLambda = 0.001;  // learning rate
    private double lambda;

    private double objectiveNumerator = 0;
    private double objectiveDenominator = 0;


    public StochasticNeighborhoodEmbedder(int rank) {
        this.rank = rank;
    }

    public synchronized void embed(final InMemorySparseMatrix m) {
        LOG.info("compressing ids");
        m.compressIds();
        LOG.info("finished compressing ids");

        // Initialize the vectors to random numbers
        vectors = new double[m.getIdMap().size()][rank];
        for (int i = 0; i < vectors.length; i++) {
            for (int j = 0; j < vectors[i].length; j++) {
                vectors[i][j] = random.nextDouble() / 10.0;
            }
            WbMathUtils.normalize(vectors[i]);
        }
        lambda = initialLambda;

        for (int iter = 0; iter < 100; iter++) {
            LOG.info("beginning iteration " + iter);

            objectiveNumerator = 0.0;
            objectiveDenominator = 0.01;

            ParallelForEach.range(0, WpThreadUtils.getMaxThreads(),
                    new Procedure<Integer>() {
                        @Override
                        public void call(Integer i) throws Exception {
                            doThread2(m, i);
                        }
                    });
            for (double v[] : vectors) {
                WbMathUtils.normalize(v);
            }
            System.err.println("err2 is " + objectiveNumerator / objectiveDenominator);
            try {
                debug(m);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            } catch (DaoException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stochastic neighbor embedding
     * @param m
     * @param threadId
     */
    private void doThread1(InMemorySparseMatrix m, int threadId) {
        int t = WpThreadUtils.getMaxThreads();
        for (int i = threadId; i < m.rowIds.length; i += t) {
            if (i % 10000 == 0) {
                LOG.info("doing " + i + ", " + (objectiveNumerator / objectiveDenominator) + ", lambda: " + lambda);
            }
            double[] v1 = vectors[m.rowIds[i]];
            int j = 0;
            double objNum = 0;
            double objDen = 0;
            for (int col = 0; col < vectors.length; col++) {
                double actual = 0.8;
                if (j < m.colIds[i].length && m.colIds[i][j] == col) {
                    actual = 1.0 - m.values[i][j];
                    j++;
                }
                if (col == i) continue;

                double[] v2 = vectors[col];
                double estimated = WbMathUtils.distance(v1, v2);

                if (actual < threshold || estimated < actual) {
                    objNum += (actual - estimated) * (actual - estimated) / actual;
                    double k = lambda * (actual - estimated) / (estimated + 1.0E-10);
                    for (int p = 0; p < rank; p++) {
                        v2[p] = v2[p] + k * (v2[p] - v1[p]);
                    }
                }
                objDen += actual;
            }
            lambda *= (1.0 - 1E-5);
            if (j != m.colIds[i].length) {
                throw new IllegalStateException();
            }
            updateObjective(objNum, objDen);
        }
    }

    /**
     * Loss function:
     *
     * L = (S[i,j] - w[i]w[j])^2 / (1 - min(S[i,j], w[i]w[j]))
     * d/dw[i](L) = - 1 / (1 - S[i,j]) * 2(S[i,j] - w[i]w[j]) w[j]
     *
     * @param m
     * @param threadId
     */
    private void doThread2(InMemorySparseMatrix m, int threadId) {
        int t = WpThreadUtils.getMaxThreads();
        for (int i = threadId; i < m.rowIds.length; i += t) {
            if (i % 10000 == 0) {
                LOG.info("doing " + i + ", " + (objectiveNumerator / objectiveDenominator) + ", lambda: " + lambda);
            }
            double[] v1 = vectors[m.rowIds[i]];
            int j = 0;
            double objNum = 0;
            double objDen = 0;
            for (int col = 0; col < vectors.length; col++) {
                double actual = 0.3;
                if (j < m.colIds[i].length && m.colIds[i][j] == col) {
                    actual = m.values[i][j];
                    j++;
                }

                double[] v2 = vectors[col];
                double estimated = WbMathUtils.dot(v1, v2);

                double den = 10E-3 + Math.max(0, 1 - actual); // distance
                double err = actual - estimated;
                objNum += err * err / den;
                objDen += 1.0 / den;
                double k = lambda * 2 / den;
                for (int p = 0; p < rank; p++) {
                    double x1 = v1[p];
                    double x2 = v2[p];
                    v2[p] += k * err * x1;
                    v1[p] += k * err * x2;
                }
            }
//            System.err.println(objNum + "," +  objDen + "," + objectiveNumerator / objectiveDenominator);
//            lambda *= (1.0 - 1E-5);
            if (j != m.colIds[i].length) {
                throw new IllegalStateException();
            }
            updateObjective(objNum, objDen);
        }
    }

    private void debug(InMemorySparseMatrix m) throws ConfigurationException, DaoException, IOException {
        List<String> queries = Arrays.asList(
            "John Coltrane",
            "Squash (plant)",
            "Squash (sport)"
        );
        int denseToRaw[] = new int[m.idMap.size()];
        for (int rawId : m.idMap.keys()) {
            int denseId = m.idMap.get(rawId);
            if (denseId >= denseToRaw.length) throw new IllegalStateException();
            denseToRaw[denseId] = rawId;
        }
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        for (String q : queries) {
            LocalPage p = pageDao.getByTitle(Language.SIMPLE, q);
            int id = m.idMap.get(p.getLocalId());
            int neighbors[] = getNeighbors(id, 10);
            System.out.println("Results for " + q + " are:");
            for (int i = 0; i < 10; i++) {
                int id2 = neighbors[i];
                System.out.println("\testimated: " + i + ". " + pageDao.getById(Language.SIMPLE, denseToRaw[id2]));
            }
            int actual[] = actualNeighbors(m, id, 10);
            for (int i = 0; i < 10; i++) {
                int id2 = actual[i];
                System.out.println("\tactual: " + i + ". " + pageDao.getById(Language.SIMPLE, denseToRaw[id2]));
            }
        }
    }


    private int[] getNeighbors(int id, int numNeighbors) throws IOException {
        double v1[] = vectors[id];
        Scoreboard<Integer> neighbors = new Scoreboard<Integer>(numNeighbors);
        for (int i = 0; i < vectors.length; i++) {
            double sim = WbMathUtils.dot(v1, vectors[i]);
            neighbors.add(i, sim);
        }
        int result[] = new int[numNeighbors];
        for (int i = 0; i < numNeighbors; i++) {
            result[i] = neighbors.getElement(i);
        }
        return result;
    }

    private int[] actualNeighbors(InMemorySparseMatrix m, int id, int numNeighbors) {
        Scoreboard<Integer> neighbors = new Scoreboard<Integer>(numNeighbors);
        for (int i = 0; i < m.colIds[id].length; i++) {
            neighbors.add(m.colIds[id][i], m.values[id][i]);
        }
        int result[] = new int[numNeighbors];
        for (int i = 0; i < numNeighbors; i++) {
            result[i] = neighbors.getElement(i);
        }
        return result;
    }

    private void updateObjective(double num, double den) {
        objectiveNumerator += num;
        objectiveDenominator += den;
    }


    public static void main (String args[]) throws ConfigurationException, IOException {
        StochasticNeighborhoodEmbedder f = new StochasticNeighborhoodEmbedder(80);
        f.env = EnvBuilder.envFromArgs(args);
        File dir = new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/");
        File input = FileUtils.getFile(dir, "symMostSimilar.matrix");
        File output = FileUtils.getFile(dir, "factorized.matrix");
        InMemorySparseMatrix m = new InMemorySparseMatrix(input);
        f.embed(m);
    }
}
