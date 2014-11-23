package org.wikibrain.utils;

import gnu.trove.set.TIntSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class TestAtomicIntSet {
    @Test
    public void testNoExpand() {
        for (int j = 0; j < 100; j++) {
            Random random = new Random();
            TIntSet actual = new TIntHashSet();
            AtomicIntSet set = new AtomicIntSet(1000);

            for (int i = 0; i < 499; i++) {
                int l = random.nextInt();
                set.add(l);
                actual.add(l);
            }

            assertEquals(actual.size(), set.size());
            for (int v : actual.toArray()) {
                assertTrue(set.contains(v));
            }
        }
    }

    @Test
    public void testExpand() {
        for (int j = 0; j < 100; j++) {
            Random random = new Random();
            TIntSet actual = new TIntHashSet();
            AtomicIntSet set = new AtomicIntSet(1000);

            for (int i = 0; i < 100; i++) {
                int l = random.nextInt();
                set.add(l);
                actual.add(l);
            }

            assertEquals(actual.size(), set.size());
            for (int v : actual.toArray()) {
                assertTrue(set.contains(v));
            }
        }
    }

    @Ignore
    @Test
    public void benchmark() throws InterruptedException {
        final int n = 50 * 1000000;
        final int nThreads = 4;
        final AtomicIntSet set = new AtomicIntSet();
//        final TLongSet set = TCollections.synchronizedSet(new TLongHashSet(2 * n));
//        final TLongSet set = new TLongHashSet(2 * n);
        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < nThreads; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < n / nThreads; i++) {
                        set.add(random.nextInt());
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

        long now = System.currentTimeMillis();
        System.out.println("elapsed is " + (now - start) + " for " + set.size());
    }
}
