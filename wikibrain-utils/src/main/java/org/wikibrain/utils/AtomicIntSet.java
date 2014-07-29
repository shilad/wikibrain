package org.wikibrain.utils;

import gnu.trove.impl.PrimeFinder;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * An implement of a set containing int that uses open addressing
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
public class AtomicIntSet {

    // Hack: pick a value that's unlikley to be used as the unused value.
    private final int unusedValue = Integer.MIN_VALUE + 1;

    private volatile AtomicIntegerArray set;

    private final AtomicInteger numElements = new AtomicInteger();

    private double loadFactor = 0.5;

    public AtomicIntSet() {
        this(5);
    }

    /**
     * Create a new set with the specified capacity.
     * Ideally, the capacity will be at least the number of total elements * (1 / loadFactor)
     * to prevent future expansions.
     *
     * @param capacity
     */
    public AtomicIntSet(int capacity) {
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
    public boolean contains(int value) {
        // An implementation of Knuth's open addressing algorithm adapted from Trove's TLongHash.
        // Returns whether the set contained the value at the *start* of the call
        AtomicIntegerArray tmp = set;      // could change out from under us...
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
            int v = tmp.get(index);
            if (v == unusedValue) {
                return false;
            }  else if (v == value) {
                return true;
            }
        } while (index != firstIndex);

        return false;
    }


    /**
     * Adds the specified value to the set.
     * @param value
     */
    public void add(int value) {
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
    private void setInternal(AtomicIntegerArray array, int value) {
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
            AtomicIntegerArray newSet = makeEmptyArray(newSize);
            for (int i = 0; i < set.length(); i++) {
                int v = set.get(i);
                if (v != unusedValue) {
                    setInternal(newSet, v);
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
    public int[] toArray() {
        TIntArrayList vals = new TIntArrayList();
        AtomicIntegerArray tmp = set;      // could change out from under us...
        for (int i = 0; i < tmp.length(); i++) {
            int v = tmp.get(i);
            if (v != unusedValue) {
                vals.add(v);
            }
        }
        return vals.toArray();
    }

    /**
     * From trove
     * @param value
     * @return
     */
    public static int hash(int value) {
        return value & 0x7fffffff;
    }

    /**
     * Creates an empty array whose capacity is a prime bigger than the requested size.
     * The array is filled with the unusedValue.
     *
     * @param capacity
     * @return
     */
    private AtomicIntegerArray makeEmptyArray(int capacity) {
        capacity = Math.max(capacity, 5);
        capacity = PrimeFinder.nextPrime(capacity);
        AtomicIntegerArray set = new AtomicIntegerArray(capacity);
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
        AtomicIntegerArray tmp = set;      // could change out from under us...
        for (int i = 0; i < tmp.length(); i++) {
            tmp.set(i, unusedValue);
        }
    }
}
