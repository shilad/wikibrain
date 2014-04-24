package org.wikibrain.utils;

import org.junit.Test;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class TestParallelForEach {

    @Test
    public void testIterator() {
        long expected = 0;
        final AtomicLong actual = new AtomicLong();
        final AtomicLong z = new AtomicLong();

        List<Integer> range = range(10, 100000);
        for (int i : range) {
            expected += i;
        }

        ParallelForEach.iterate(
                range.iterator(),
                5,
                10,
                new Procedure<Integer>() {
                    @Override
                    public void call(Integer arg) throws Exception {
                        actual.addAndGet(arg);
                        // do something a little slow
                        for (int i = 0; i < 100; i++)
                            z.addAndGet(1);
                    }
                },
                10000
        );

        assertEquals(expected, actual.get());
    }


    /**
     * @param begin inclusive
     * @param end exclusive
     * @return list of integers from begin to end
     */
    public static List<Integer> range(final int begin, final int end) {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return begin + index;
            }

            @Override
            public int size() {
                return end - begin;
            }
        };
    }
}
