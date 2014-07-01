package org.wikibrain.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Based on http://stackoverflow.com/a/371043/141245, but threadsafe.
 * Represents an iterator for the range [min, max) (exclusive upper limit).
 */
public class IntRangeIterator implements Iterator<Integer> {
    private final AtomicInteger nextValue;
    private final int max;

    /**
     * Creates an iterator for the range [min, max) (exclusive upper limit).
     * @param min
     * @param max
     */
    public IntRangeIterator(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        this.nextValue = new AtomicInteger(min);
        this.max = max;
    }

    public boolean hasNext() {
        return nextValue.get() < max;
    }

    public Integer next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return nextValue.getAndIncrement();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}