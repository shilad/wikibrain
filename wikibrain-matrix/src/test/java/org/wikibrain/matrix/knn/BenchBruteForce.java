package org.wikibrain.matrix.knn;

import org.wikibrain.matrix.DenseMatrix;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class BenchBruteForce {
    public static void main(String args[]) throws IOException {
        DenseMatrix matrix = TestUtils.createMatrix(100000, 400);
        BruteForceKNNFinder f = new BruteForceKNNFinder(matrix);
        long before = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            System.out.println("doing " + i);
            float [] vector = TestUtils.randomVector(400);
            f.query(vector, 10, 10, null);
        }
        long after = System.currentTimeMillis();
        System.out.println("millis is " + (after-before) / 100.0);
    }

}
