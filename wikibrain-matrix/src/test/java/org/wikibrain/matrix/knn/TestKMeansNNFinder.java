package org.wikibrain.matrix.knn;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Test;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;
import org.wikibrain.matrix.DenseMatrixWriter;
import org.wikibrain.matrix.ValueConf;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class TestKMeansNNFinder {
    DenseMatrix matrix;
    KNNFinder finder;

    private void makeSmall() throws IOException {
        matrix = createMatrix(1000, 20);
        KmeansKNNFinder km = new KmeansKNNFinder(matrix);
        km.setMaxLeaf(10);
        km.setSampleSize(200);
        km.setBranchingFactor(5);
        km.build();
        finder = km;
    }

    private void makeBig() throws IOException {
        matrix = createMatrix(100000, 20);
        KmeansKNNFinder km = new KmeansKNNFinder(matrix);
        km.setMaxLeaf(10);
        km.setSampleSize(10000);
        km.setBranchingFactor(5);
        km.build();
        finder = km;
    }

    @Test
    public void testBuild() throws IOException {
        makeSmall();
    }

    @Test
    public void testQueryCoverage() throws IOException {
        makeSmall();
        int hits = 0;
        int iters = 10;
        for (int i = 0; i < iters; i++) {
            System.out.println("doing " + i);
            float[] v = randomVector(20);
            Neighborhood estimated = finder.query(v, 10, 1000, null);
            Neighborhood actual = actualNeighbors(v, matrix, 10);
            hits += overlap(estimated, actual);
        }
        assertEquals(iters * 10, hits);
    }

    @Test
    public void testQuery() throws IOException {
        makeBig();
        int hits = 0;
        long elapsedTree = 0;
        long elapsedBruteForce = 0;
        int iters = 10;
        for (int i = 0; i < iters; i++) {
            System.out.println("doing " + i);
            float[] v = randomVector(20);
            long t1 = System.currentTimeMillis();
            Neighborhood estimated = finder.query(v, 10, 2000, null);
            long t2 = System.currentTimeMillis();
            Neighborhood actual = actualNeighbors(v, matrix, 10);
            long t3 = System.currentTimeMillis();
            elapsedBruteForce += (t3 - t2);
            elapsedTree += (t2 - t1);
            hits += overlap(estimated, actual);
        }
        System.out.println("Mean overlap is " + (1.0 * hits / iters));
        System.out.println("Mean bruteforce millis is " + (1.0 * elapsedBruteForce / iters));
        System.out.println("Mean optimized millis is " + (1.0 * elapsedTree / iters));
    }

    private int overlap(Neighborhood n1, Neighborhood n2) {
        TIntSet ids = new TIntHashSet();
        for (int i = 0; i < n1.size(); i++) {
            ids.add(n1.getId(i));
        }
        int result = 0;
        for (int i = 0; i < n2.size(); i++) {
            if (ids.contains(n2.getId(i))) result++;
        }
        return result;
    }

    private static Neighborhood actualNeighbors(float [] v, DenseMatrix matrix, int n) {
        NeighborhoodAccumulator accum = new NeighborhoodAccumulator(n);
        for (DenseMatrixRow row : matrix) {
            double sim = KmeansKNNFinder.cosine(v, row);
            accum.visit(row.getRowIndex(), sim);
        }
        return accum.get();
    }

    private static DenseMatrix createMatrix(int rows, int cols) throws IOException {
        File tmp = File.createTempFile("knnfinder", ".matrix");
        tmp.delete();
        ValueConf vconf = new ValueConf();
        int [] colIds = new int[cols];
        for (int i= 0 ; i < cols; i++) { colIds[i] = i; }
        DenseMatrixWriter writer = new DenseMatrixWriter(tmp, vconf);
        for (int i = 0; i < rows; i++) {
            writer.writeRow(new DenseMatrixRow(vconf, i, colIds, randomVector(cols)));
        }
        writer.finish();
        tmp.deleteOnExit();
        return new DenseMatrix(tmp);
    }

    private static float[] randomVector(int cols) {
        Random rand = new Random();
        float [] vals = new float[cols];
        for (int j = 0; j < cols; j++) {
            vals[j] = rand.nextFloat();
        }
        return vals;
    }
}
