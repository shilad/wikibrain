package org.wikibrain.utils;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestAtomicIntInt {

    @Test
    public void testSimpleSets() throws InterruptedException {
        final int n = 100000;
        final AtomicIntIntMap map = new AtomicIntIntMap();
        final AtomicByteArray bytes = new AtomicByteArray(n * 10);
        final AtomicInteger count = new AtomicInteger();
        final AtomicLong hits = new AtomicLong();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 12; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    Random random = new Random();
                    while (count.incrementAndGet() < n) {
                        if (count.get() % 100000 == 0) {
                            System.out.println("count is " + count.get() + ", hits is " + hits.get());
                        }
                        int key = random.nextInt(bytes.length());
                        byte value = bytes.get(key);
                        while (value == 0) {
                            value = (byte) random.nextInt();
                        }
                        bytes.set(key, value);
//                        System.out.println("putting " + key + ", " + value);
                        map.put(key, value);
                    }
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        for (int i = 0; i < bytes.length(); i++) {
            byte b = bytes.get(i);
            if (b == 0) {
                assertFalse(map.containsKey(i));
            } else {
                assertEquals((int)b, map.get(i));
            }
        }
    }

    @Test
    public void testOverwrite() throws InterruptedException {
        final int n = 1000000;
        final AtomicIntIntMap map = new AtomicIntIntMap();
        final AtomicByteArray seeds = new AtomicByteArray(n/2);
        final AtomicByteArray counts = new AtomicByteArray(n/2);
        final AtomicInteger count = new AtomicInteger();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 12; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    Random random = new Random();
                    while (count.incrementAndGet() < n) {
                        if (count.get() % 100000 == 0) {
                            System.out.println("count is " + count.get());
                        }
                        int key = random.nextInt(seeds.length());
                        byte seed = seeds.get(key);
                        while (seed == 0) {
                            seed = (byte) random.nextInt();
                        }
                        seeds.set(key, seed);
                        int value = seed;
                        for (int j = 0; j < counts.getAndIncrement(key); j++) {
                            value *= 31;
                        }
//                        System.out.println("putting " + key + ", " + value);
                        map.put(key, value);
                    }
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        int [] hits = new int[10];
        for (int i = 0; i < seeds.length(); i++) {
            byte s = seeds.get(i);
            if (s == 0) {
                assertFalse(map.containsKey(i));
                continue;
            }
            byte c = counts.get(i);
            int value = map.get(i);
            int v = s;
            for (int j = 0; j < c; j++) {
                if (v == value) {
                    hits[c - j]++;
                }
                v *= 31;
            }
        }

        System.out.println("hits are " + Arrays.toString(hits));
    }

    @Ignore
    @Test
    public void testHeavyLoad() throws InterruptedException {
        final int n = 10000000;
//        final int n = 1000;
        final AtomicInteger count = new AtomicInteger();
        final AtomicIntIntMap map = new AtomicIntIntMap();
        final AtomicLong hits = new AtomicLong();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 8; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    Random random = new Random();
                    while (count.incrementAndGet() < n) {
                        if (count.get() % 100000 == 0) {
                            System.out.println("count is " + count.get() + ", hits is " + hits.get());
                        }
                        int key = random.nextInt(count.get());
                        int value = random.nextInt();
//                        System.out.println("putting " + key + ", " + value);
                        map.put(key, value);
                        for (int i = 0; i < 100; i++) {
                            if (map.get(random.nextInt(count.get())) != -1) {
                                hits.incrementAndGet();
                            }
                        }
                    }
                }
            });
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long end = System.currentTimeMillis();
        System.err.println("made " + count.get() + " ops " + " in " + (end - start) + ", final size is " + map.size());
    }
}
