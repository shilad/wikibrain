package org.wikibrain.matrix.knn;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Test;
import org.wikibrain.matrix.DenseMatrix;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class TestKDDNNFinder {
    DenseMatrix matrix;
    KNNFinder finder;

    private void makeSmall() throws IOException {
        matrix = TestUtils.createMatrix(1000, 20);
        KDTreeKNN rp = new KDTreeKNN(matrix);
        rp.build();
        finder = rp;
    }

    private void makeBig() throws IOException {
        matrix = TestUtils.createMatrix(40000, 100);
        KDTreeKNN rp = new KDTreeKNN(matrix);
        rp.build();
        finder = rp;
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
            float[] v = TestUtils.randomVector(20);
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
        int iters = 100;
        for (int i = 0; i < iters; i++) {
            float[] v = TestUtils.randomVector(100);
            long t1 = System.currentTimeMillis();
            Neighborhood estimated = finder.query(v, 10, 200, null);
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

    private static Neighborhood actualNeighbors(float [] v, DenseMatrix matrix, int n) throws IOException {
        BruteForceKNNFinder f = new BruteForceKNNFinder(matrix);
        return f.query(v, n, n, null);
    }

}
