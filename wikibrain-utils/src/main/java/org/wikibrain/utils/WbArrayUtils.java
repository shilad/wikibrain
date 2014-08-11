package org.wikibrain.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

/**
 */
public class WbArrayUtils {
    static public int[] grow(int A[], int amount) {
        int B[] = new int[A.length + amount];
        System.arraycopy(A, 0, B, 0, A.length);
        return B;
    }

    static public String[] grow(String A[], int amount) {
        String B[] = new String[A.length + amount];
        System.arraycopy(A, 0, B, 0, A.length);
        return B;
    }

    static public List<Integer> toList(int A[]) {
        return Arrays.asList(ArrayUtils.toObject(A));
    }


}
