package org.wikapidia.utils;

/**
 */
public class WpArrayUtils {
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
}
