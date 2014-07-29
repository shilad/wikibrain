package org.wikibrain.utils;

import gnu.trove.impl.PrimeFinder;
import gnu.trove.list.array.TLongArrayList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * An implement of a set containing longs that uses open addressing
 *
 * The implementation is threadsafe and almost entirely lock-free.
 * Locks do occur while the the underlying array is expanded.
 *
 * If you're careful and spread an accurate guess of capacity,
 * peformance will be dramatically improved.
 *
 * Does not support removals at the moment.
 *
 * @author Shilad Sen
 */
public class AtomicLongSet {

    // Hack: pick a value that's unlikley to be used as the unused value.
    private final long unusedValue = Long.MIN_VALUE + 1;

    private volatile AtomicLongArray set;

    private final AtomicInteger numElements = new AtomicInteger();

    private double loadFactor = 0.5;

    public AtomicLongSet() {
        this(5);
    }

    /**
     * Create a new set with the specified capacity.
     * Ideally, the capacity will be at least the number of total elements * (1 / loadFactor)
     * to prevent future expansions.
     *
     * @param capacity
     */
    public AtomicLongSet(int capacity) {
        set = makeEmptyArray(capacity);
    }

    /**
     * Returns true iff the set contains the specified value.
     * If the value is added before contains is called, it will always return true.
     * If the value is added while contains is called, it may or may not return true.
     * Will always return false if the set does not contain the value.
     *
     * @param value
     * @return
     */
    public boolean contains(long value) {
        // An implementation of Knuth's open addressing algorithm adapted from Trove's TLongHash.
        // Returns whether the set contained the value at the *start* of the call
        AtomicLongArray tmp = set;      // could change out from under us...
        int length = tmp.length();
        int hash = hash(value);
        int probe = 1 + (hash % (length - 2));
        int index = hash % length;
        int firstIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            long l = tmp.get(index);
            if (l == unusedValue) {
                return false;
            }  else if (l == value) {
                return true;
            }
        } while (index != firstIndex);

        return false;
    }


    /**
     * Adds the specified value to the set.
     * @param value
     */
    public void add(long value) {
        if (value == unusedValue) {
            throw new IllegalArgumentException("Value " + value + " is used internally as an unused slot marker!");
        }

        numElements.incrementAndGet();
        expandIfNecessary();

        setInternal(set, value);
    }

    /**
     * Store a particular value in an array representing a set.
     * @param array
     * @param value
     */
    private void setInternal(AtomicLongArray array, long value) {
        // An implementation of Knuth's open addressing algorithm adapted from Trove's TLongHash.
        int length = array.length();
        int hash = hash(value);
        int probe = 1 + (hash % (length - 2));
        int index = hash % length;
        int firstIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            long v = array.get(index);
            if (v == value) {
                break; // already set
            } else if (v == unusedValue && array.compareAndSet(index, unusedValue, value)) {
                break;
            }
        } while (index != firstIndex);
    }

    /**
     * Returns the number of elements stored in the set.
     * @return
     */
    public int size() {
        return numElements.get();
    }

    /**
     * Expand the underlying array if the load factor is exceeded.
     * If the load factor is NOT exceeded, no locking is required.
     * If it is exceeded, all threads block while one expands.
     */
    private void expandIfNecessary() {
        // Check if we're safe (usually the case, so no locks typically used!)
        if (numElements.get() < loadFactor * set.length()) {
            return;
        }
        synchronized (numElements) {
            // Maybe somebody expanded while we were waiting for the lock
            if (numElements.get() < loadFactor * set.length()) {
                return;
            }

            // expand, rehash
            int newSize = (int) Math.ceil(set.length() / loadFactor);
            AtomicLongArray newSet = makeEmptyArray(newSize);
            for (int i = 0; i < set.length(); i++) {
                long l = set.get(i);
                if (l != unusedValue) {
                    setInternal(newSet, l);
                }
            }
            set = newSet;
        }
    }

    /**
     * Returns the values in the set.
     * This is a relatively expensive O(n) operation.
     * It will return all the elements in the set at the start of the call, and
     * it may return any (or none of) the elements added while it is ongoing.
     *
     * @return the values in the set.
     */
    public long[] toArray() {
        TLongArrayList vals = new TLongArrayList();
        AtomicLongArray tmp = set;      // could change out from under us...
        for (int i = 0; i < tmp.length(); i++) {
            long l = tmp.get(i);
            if (l != unusedValue) {
                vals.add(l);
            }
        }
        return vals.toArray();
    }

    /**
     * From trove
     * @param value
     * @return
     */
    public static int hash(long value) {
        return ((int)(value ^ (value >>> 32))) & 0x7fffffff;
    }

    /**
     * Creates an empty array whose capacity is a prime bigger than the requested size.
     * The array is filled with the unusedValue.
     *
     * @param capacity
     * @return
     */
    private AtomicLongArray makeEmptyArray(int capacity) {
        capacity = Math.max(capacity, 5);
        capacity = PrimeFinder.nextPrime(capacity);
        AtomicLongArray set = new AtomicLongArray(capacity);
        for (int i = 0; i < capacity; i++) {
            set.set(i, unusedValue);
        }
        return set;
    }

    /**
     * Removes all elements in the set.
     * Does not compact it.
     */
    public void clear() {
        AtomicLongArray tmp = set;      // could change out from under us...
        for (int i = 0; i < tmp.length(); i++) {
            tmp.set(i, unusedValue);
        }
    }
}
