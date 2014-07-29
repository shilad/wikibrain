package org.wikibrain.matrix;

/**
 * @author Shilad Sen
 */
public class SparseMatrixUtils {

    static public boolean isIncreasing(int A[]) {
        int lastId = Integer.MIN_VALUE;
        for (int i = 0; i < A.length; i++) {
            if (A[i] <= lastId) {
                return false;
            }
            lastId = A[i];
        }
        return true;
    }
}
