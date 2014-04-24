package org.wikibrain.utils;

/**
 * @author Shilad Sen
 */
public class WpThreadUtils {
    public static int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    public static int getMaxThreads() {
        return MAX_THREADS;
    }

    public static void setMaxThreads(int maxThreads) {
        MAX_THREADS = maxThreads;
    }
}
