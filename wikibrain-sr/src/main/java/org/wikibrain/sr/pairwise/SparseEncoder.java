package org.wikibrain.sr.pairwise;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WbMathUtils;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class SparseEncoder {

    private static final Logger LOG = Logger.getLogger(KMeansDictionaryLearner.class.getName());

    private final Random random = new Random();
    private final Env env;
    private final Language language;
    private final LocalPageDao pageDao;
    private final SRMetric metric;
    private final int[] dictionaryIds;
    private final int[] allIds;
    private final double[][] dictionarySims;
    private final TIntSet allIdSet;

    private int nextIndex = 0;
    TreeMap<Integer, double[]> rowBuffer = new TreeMap<Integer, double[]>();

    public SparseEncoder(Env env, SRMetric metric, int dictionaryIds[]) throws ConfigurationException, DaoException, IOException {
        this.env = env;
        this.metric = metric;
        this.language = metric.getLanguage();
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.dictionaryIds = dictionaryIds;
        this.allIds = getAllIds();
        Arrays.sort(this.dictionaryIds);
        Arrays.sort(allIds);
        allIdSet = new TIntHashSet(allIds);
        this.dictionarySims = new double[dictionaryIds.length][];
        readDictionarySims();
    }

    private int[] getAllIds() throws DaoException {
        TIntList allIds = new TIntArrayList();
        for (LocalId id : pageDao.getIds(DaoFilter.normalPageFilter(language))) {
            allIds.add(id.getId());
        }
        return allIds.toArray();
    }

    private void readDictionarySims() throws IOException {
        // TODO: read ids to ensure we don't get confused!

        File dict = new File("dict_sims.bin");
        if (!dict.isFile()) {
            writeCosimilarityMatrix(dict);
        }
        if (dict.isFile()) {
            DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(dict)));
            for (int i = 0; i < dictionaryIds.length; i++) {
                dictionarySims[i] = new double[allIds.length];
                for (int j = 0; j < allIds.length; j++) {
                    dictionarySims[i][j] = stream.readDouble();
                }
            }
            stream.close();
        }
    }

    public TIntDoubleMap encode(int id, int maxRank) throws DaoException {
        return encode(getSims(id), maxRank);

    }

    public TIntDoubleMap encode(double vector[], int maxRank) {
        double errors[] = Arrays.copyOf(vector, vector.length);
        TIntList chosen = new TIntArrayList();
        for (int i = 0; i < maxRank; i++) {
            int bestIndex = -1;
            double bestScore = 0;
            for (int j = 0; j < dictionaryIds.length; j++) {
                double s = WbMathUtils.dot(dictionarySims[j], errors);
//                System.out.println("sums are " + WbMathUtils.dot(vector, vector) + "," + WbMathUtils.dot(dictionarySims[j], dictionarySims[j]));
//                System.out.println("comparing " + Arrays.toString(vector) + " and " + Arrays.toString(dictionarySims[j]));
                if (bestIndex < 0 || Math.abs(s) > Math.abs(bestScore)) {
                    bestScore = s;
                    bestIndex = j;
                }
            }
            System.out.format("Chose %s with weight %.2f\n", title(dictionaryIds[bestIndex]), bestScore);
            WbMathUtils.add(-bestScore, errors, dictionarySims[bestIndex], errors);
            System.out.println("Error is " + WbMathUtils.dot(errors, errors));
            if (!chosen.contains(bestIndex)) chosen.add(bestIndex);
        }

        double Y[] = vector;
        double X[][] = new double[Y.length][chosen.size()];
        for (int i = 0; i < chosen.size(); i++) {
            for (int j = 0; j < allIds.length; j++) {
                X[j][i] = dictionarySims[chosen.get(i)][j];
            }
        }

        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(Y, X);
        double [] coeffs = regression.estimateRegressionParameters();
        double [] olsPredict = new double[vector.length];
        Arrays.fill(olsPredict, coeffs[0]);
        System.out.println("final OLS residual is " + regression.calculateResidualSumOfSquares());
        System.out.format("eq is %+.3f",  coeffs[0]);
        for (int i = 0; i < chosen.size(); i++) {
            System.out.format(" %+.3f * %s", coeffs[i+1], title(dictionaryIds[chosen.get(i)]));
            WbMathUtils.add(coeffs[i+1], olsPredict, dictionarySims[chosen.get(i)], olsPredict);
        }

        System.out.println("\nTOP ACTUAL:\n");
        printTop(vector, 100);

        double predict[] = new double[vector.length];
        WbMathUtils.add(-1, predict, vector, errors);
        System.out.println("\nTOP GREEDY PREDICT:\n");
        printTop(predict, 100);

        System.out.println("\nTOP OLS PREDICT:\n");
        printTop(olsPredict, 100);

        return null;
    }

    private void printTop(double vector[], int n) {
        Leaderboard board = new Leaderboard(n);
        for (int i = 0; i < vector.length; i++) {
            board.tallyScore(allIds[i], vector[i]);
        }
        SRResultList top = board.getTop();
        top.sortDescending();
        for (SRResult result : top) {
            System.out.format("\t%.3f %s (id %d)\n", result.getScore(), title(result.getId()), result.getId());
        }
    }

    private String title(int id) {
        try {
            return pageDao.getById(language, id).getTitle().getCanonicalTitle();
        } catch (DaoException e) {
            throw new IllegalArgumentException(e);
        }
    }



    private double[] getSims(int id) throws DaoException {
        SRResultList mostSimilar = metric.mostSimilar(id, allIds.length / 10, allIdSet);
        mostSimilar.sortDescending();
        double scores[] = new double[allIds.length];
        Arrays.fill(scores,(mostSimilar.minScore() * 0.7));
        mostSimilar.sortById();
        int k = 0;
        for (int j = 0; j < mostSimilar.numDocs(); j++) {
            int id2 = mostSimilar.getId(j);
            while (allIds[k] < id2) { k++; }
            if (allIds[k] != id2) {
                throw new IllegalStateException("Expected " + id2 + ", found " + allIds[k]);
            }
            scores[k] = (float) mostSimilar.getScore(j);
        }
        return scores;
    }

    private void writeCosimilarityMatrix(File file) throws IOException {
        nextIndex = 0;
        rowBuffer.clear();

        //Mapping a file into memory
        final double defaultValues[] = new double[allIds.length];
        Arrays.fill(defaultValues, (double) Math.sqrt(1.0 / allIds.length));

        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        ParallelForEach.range(0, dictionaryIds.length, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                try {
                    writeCosimilarities(out, i);
                } catch (Exception e) {
                    e.printStackTrace();
                    writeRow(out, i, defaultValues);
                }
            }
        });

        out.close();
    }

    private void writeCosimilarities(DataOutputStream out, int i) throws DaoException, IOException {
        if (i % 1000 == 0) {
            LOG.info("building cosimilarity for dictionary id " + i + " out of " + dictionaryIds.length + " with length " + allIds.length);
        }
        double scores[] = getSims(dictionaryIds[i]);
        WbMathUtils.normalize(scores);

        writeRow(out, i, scores);
    }

    private synchronized void writeRow(DataOutputStream out, int index, double values[]) throws IOException {
        rowBuffer.put(index, values);
        while (rowBuffer.containsKey(nextIndex)) {
            values = rowBuffer.get(nextIndex);
            for (int i = 0; i < values.length; i++) {;
                out.writeDouble(values[i]);
            }
            nextIndex++;
        }
    }

    public static void main(String args[]) throws ConfigurationException, IOException, DaoException {

        Env env = EnvBuilder.envFromArgs(args);
        Language language = env.getLanguages().getDefaultLanguage();
        SRMetric metric = env.getConfigurator().get(SRMetric.class, "milnewitten", "language", language.getLangCode());

        TIntSet concepts = new TIntHashSet();
        String conceptPath = "dictionary_ids.txt";
        for (String line : FileUtils.readLines(new File(conceptPath))) {
            concepts.add(Integer.valueOf(line.trim()));
        }

        SparseEncoder encoder = new SparseEncoder(env, metric, concepts.toArray());
        encoder.encode(218283, 50);
    }


}
