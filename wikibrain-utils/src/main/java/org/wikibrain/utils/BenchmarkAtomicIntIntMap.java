package org.wikibrain.utils;

import gnu.trove.TCollections;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Shilad Sen
 */
public class BenchmarkAtomicIntIntMap {
    private static final double fractionGets = 0.75;

    public static void testMap(final IntIntMap map, final int keys[], int numThreads) throws InterruptedException {
        final AtomicInteger count = new AtomicInteger();
        final AtomicInteger hits = new AtomicInteger();
        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < numThreads; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    Random random = new Random();
                    while (count.getAndIncrement() < keys.length * 10) {
                        int key = keys[random.nextInt(keys.length)];
                        if (random.nextDouble() < fractionGets) {
                            if (map.get(key) != -1) {
                                hits.incrementAndGet();
                            }
                        } else {
                            map.put(key, random.nextInt());
                        }
                    }
                }
            });
        }

        long before = System.currentTimeMillis();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long after = System.currentTimeMillis();
        long elapsed = (after - before);
        System.out.println("ops per second is " + (keys.length * 10.0 / (elapsed / 1000.0)));
    }

    public static void main(String args[]) throws InterruptedException {
        int size = Integer.valueOf(args[0]);
        int threads = Integer.valueOf(args[1]);

        Random random = new Random();
        TIntSet keySet = new TIntHashSet();
        while (keySet.size() < size) {
            keySet.add(random.nextInt());
        }
        int keys[] = keySet.toArray();

        System.out.println("testing trove");
        for (int i = 0; i < 5; i++) {
            testMap(new TroveMap(), keys, threads);
        }
        System.out.println("\ntesting atomic map");
        for (int i = 0; i < 5; i++) {
            testMap(new AtomicMap(), keys, threads);
        }
        System.out.println("\ntesting concurrent map");
        for (int i = 0; i < 5; i++) {
            testMap(new ConcurrentMap(), keys, threads);
        }
    }

    public static interface IntIntMap {
        public int get(int key);
        public void put(int key, int value);
    }

    public static class TroveMap implements IntIntMap {
        TIntIntMap map = TCollections.synchronizedMap(new TIntIntHashMap());
        public int get(int key) { return map.get(key); }
        public void put(int key, int value) { map.put(key, value); }
    }

    public static class AtomicMap implements IntIntMap {
        AtomicIntIntMap map = new AtomicIntIntMap();
        public int get(int key) { return map.get(key); }
        public void put(int key, int value) { map.put(key, value); }
    }

    public static class ConcurrentMap implements IntIntMap {
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<Integer, Integer>();
        public int get(int key) {
            Integer v = map.get(key);
            return (v == null) ? -1 : v;
        }
        public void put(int key, int value) { map.put(key, value); }
    }
}
