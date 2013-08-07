package org.wikapidia.utils;


import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 * Utilities to run for each loops in parallel.
 */
public class ParallelForEach {
    public static final Logger LOG = Logger.getLogger(ParallelForEach.class.getName());

    /**
     * Construct a parallel loop on [from, to).
     *
     * @param from bottom of range (inclusive)
     * @param to top of range (exclusive)
     * @param numThreads
     * @param fn callback
     */
    public static void range(int from, int to, int numThreads, final Procedure<Integer> fn) {
        List<Integer> range = new ArrayList<Integer>();
        for (int i = from; i < to; i++) { range.add(i); }
        loop(range, numThreads, fn);
    }
    public static void range(int from, int to, final Procedure<Integer> fn) {
        range(from, to, WpThreadUtils.getMaxThreads(), fn);
    }
    public static <T,R> List<R> range(int from, int to, int numThreads, final Function<Integer, R> fn) {
        List<Integer> range = new ArrayList<Integer>();
        for (int i = from; i < to; i++) { range.add(i); }
        return loop(range, numThreads, fn);
    }
    public static <T,R> List<R> range(int from, int to, final Function<Integer, R> fn) {
        return range(from, to, WpThreadUtils.getMaxThreads(), fn);
    }

    public static <T,R> List<R> loop(
            Collection<T> collection,
            int numThreads,
            final Function<T,R> fn) {
        return loop(collection, numThreads, fn, 50);
    }
    public static <T,R> List<R> loop(
            Collection<T> collection,
            final Function<T,R> fn) {
        return loop(collection, WpThreadUtils.getMaxThreads(), fn, 50);
    }
    public static <T> void loop(
            Collection<T> collection,
            final Procedure<T> fn) {
        loop(collection, WpThreadUtils.getMaxThreads(), fn, 50);
    }
    public static <T> void loop(
            Collection<T> collection,
            int numThreads,
            final Procedure<T> fn) {
        loop(collection, numThreads, fn, 50);
    }
    public static <T> void loop(
            Collection<T> collection,
            int numThreads,
            final Procedure<T> fn,
            final int logModulo) {
        loop(collection, numThreads, new Function<T, Object> () {
            public Object call(T arg) throws Exception {
                fn.call(arg);
                return null;
            }
        }, logModulo);
    }
    public static <T> void loop(
            Collection<T> collection,
            final Procedure<T> fn,
            final int logModulo) {
        loop(collection, WpThreadUtils.getMaxThreads(), new Function<T, Object> () {
            public Object call(T arg) throws Exception {
                fn.call(arg);
                return null;
            }
        }, logModulo);
    }
    public static <T,R> List<R> loop(
            Collection<T> collection,
            final Function<T,R> fn,
            final int logModulo) {
        return loop(collection, WpThreadUtils.getMaxThreads(), fn, logModulo);
    }

    public static <T,R> List<R> loop(
            Collection<T> collection,
            int numThreads,
            final Function<T,R> fn,
            final int logModulo) {
        final List<R> result = new ArrayList<R>();
        for (int i = 0; i < collection.size(); i++) result.add(null);
        final ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch latch = new CountDownLatch(collection.size());
        try {
            // create a copy so that modifications to original list are safe
            final List<T> asList = new ArrayList<T>(collection);
            for (int i = 0; i < asList.size(); i++) {
                final int finalI = i;
                exec.submit(new Runnable() {
                    public void run() {
                        T obj = asList.get(finalI);
                        try {
                            if (finalI % logModulo == 0) {
                                LOG.info("processing list element " + (finalI+1) + " of " + asList.size());
                            }
                            R r = fn.call(obj);
                            result.set(finalI, r);
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "error processing list element " + obj, e);
                            LOG.log(Level.SEVERE, "stacktrace: " + ExceptionUtils.getStackTrace(e).replaceAll("\n", " ").replaceAll("\\s+", " "));
                        } finally {
                            latch.countDown();
                        }
                    }});
            }
            latch.await();
            return result;
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Interrupted parallel for each", e);
            throw new RuntimeException(e);
        } finally {
            exec.shutdown();
        }

    }
}
