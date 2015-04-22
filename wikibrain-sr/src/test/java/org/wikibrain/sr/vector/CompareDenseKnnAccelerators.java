package org.wikibrain.sr.vector;

import gnu.trove.set.TIntSet;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.knn.KDTreeKNN;
import org.wikibrain.matrix.knn.KNNFinder;
import org.wikibrain.matrix.knn.KmeansKNNFinder;
import org.wikibrain.matrix.knn.RandomProjectionKNNFinder;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResultList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class CompareDenseKnnAccelerators {
    private final DenseVectorSRMetric sr;
    private final Env env;
    private final DenseMatrix matrix;
    private int sampleSize = 10;

    public CompareDenseKnnAccelerators(Env env) throws ConfigurationException {
        this.env =  env;
        this.sr = (DenseVectorSRMetric) env.getConfigurator().get(SRMetric.class, "word2vec", "language", env.getDefaultLanguage().getLangCode());
        this.matrix = sr.getGenerator().getFeatureMatrix();
    }

    public void evaluateRandomProjections() throws IOException {
        RandomProjectionKNNFinder rp = new RandomProjectionKNNFinder(matrix);
        rp.build();
        evaluate(rp);
    }

    public void evaluateKMeansTree() throws IOException {
        KmeansKNNFinder rp = new KmeansKNNFinder(matrix);
        rp.build();
        evaluate(rp);
    }

    public void evaluateKDTree() throws IOException {
        KDTreeKNN rp = new KDTreeKNN(matrix);
        rp.build();
        evaluate(rp);
    }

    public void evaluate(KNNFinder finder) throws IOException {
        for (int multiplier : Arrays.asList(1, 5, 10, 20, 50, 100, 1000)) {
            for (int k : Arrays.asList(1, 10, 20, 50, 100, 1000)) {
                evaluate(finder, k, multiplier);
            }
        }
    }

    public void evaluate(KNNFinder finder, int k, int multiplier) throws IOException {
        sr.setAcceleratorMultiplier(multiplier);
        Random rand = new Random();
        long elapsedEstimated = 0;
        long elapsedActual = 0;

        int total = 0;
        int hits = 0;

        for (int i = 0; i < sampleSize; i++) {
            int id = matrix.getRowIds()[rand.nextInt(matrix.getNumRows())];
            float [] vec = matrix.getRow(id).getValues();
            long t1 = System.currentTimeMillis();
            sr.setAccelerator(finder);
            SRResultList estimated = sr.mostSimilar(vec, k, null);
            long t2 = System.currentTimeMillis();
            sr.setAccelerator(null);
            SRResultList actual = sr.mostSimilar(vec, k, null);
            long t3 = System.currentTimeMillis();
            if (actual == null || estimated == null) {
                continue;
            }

            TIntSet overlap = actual.asTroveMap().keySet();
            overlap.retainAll(estimated.asTroveMap().keySet());
            total += actual.numDocs();
            hits += overlap.size();

            elapsedEstimated += (t2 - t1);
            elapsedActual += (t3 - t2);
        }

        System.out.format(
                "Results for k=%d with multiplier=%d: Precision %3f, accel millis=%3f naive millis=%3f ratio=%.3f\n",
                k, multiplier, 1.0 * hits / total,
                1.0 * elapsedEstimated / sampleSize, 1.0 * elapsedActual / sampleSize,
                1.0 * elapsedActual / elapsedEstimated
        );
    }

    public static void main(String args[]) throws ConfigurationException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        CompareDenseKnnAccelerators cmp = new CompareDenseKnnAccelerators(env);
        cmp.evaluateKDTree();
        cmp.evaluateRandomProjections();
//        cmp.evaluateKMeansTree();
    }
}
