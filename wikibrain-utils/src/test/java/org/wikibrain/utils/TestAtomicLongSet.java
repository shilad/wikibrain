package org.wikibrain.utils;

import gnu.trove.TCollections;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestAtomicLongSet {
    @Test
    public void testNoExpand() {
        for (int j = 0; j < 100; j++) {
            Random random = new Random();
            TLongSet actual = new TLongHashSet();
            AtomicLongSet set = new AtomicLongSet(1000);

            for (int i = 0; i < 499; i++) {
                long l = random.nextLong();
                set.add(l);
                actual.add(l);
            }

            assertEquals(actual.size(), set.size());
            for (long v : actual.toArray()) {
                assertTrue(set.contains(v));
            }

            long keys1[] = actual.toArray();
            long keys2[] = set.toArray();
            Arrays.sort(keys1);
            Arrays.sort(keys2);
        }
    }

    @Test
    public void testExpand() {
        for (int j = 0; j < 100; j++) {
            Random random = new Random();
            TLongSet actual = new TLongHashSet();
            AtomicLongSet set = new AtomicLongSet();

            for (int i = 0; i < 100; i++) {
                long l = random.nextLong();
                set.add(l);
                actual.add(l);
            }

            assertEquals(actual.size(), set.size());
            for (long v : actual.toArray()) {
                assertTrue(set.contains(v));
            }

            long keys1[] = actual.toArray();
            long keys2[] = set.toArray();
            Arrays.sort(keys1);
            Arrays.sort(keys2);
        }
    }

    @Ignore
    @Test
    public void benchmark() throws InterruptedException {
        final int n = 50 * 1000000;
        final int nThreads = 4;
        final AtomicLongSet set = new AtomicLongSet();
//        final TLongSet set = TCollections.synchronizedSet(new TLongHashSet(2 * n));
//        final TLongSet set = new TLongHashSet(2 * n);
        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < nThreads; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < n / nThreads; i++) {
                        set.add(random.nextLong());
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
