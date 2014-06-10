package org.wikibrain.utils;

import gnu.trove.impl.PrimeFinder;
import gnu.trove.list.array.TIntArrayList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * An implementation of an int int map that uses open addressing
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
public class AtomicIntIntMap {

    // Hack: pick a value that's unlikley to be used as the unused value.
    private final int unusedKey = Integer.MIN_VALUE + 1;

    private volatile AtomicIntegerArray keys;
    private volatile int[] vals;

    private final AtomicInteger numElements = new AtomicInteger();

    private double loadFactor = 0.5;

    public AtomicIntIntMap() {
        this(5);
    }

    /**
     * Create a new keys with the specified capacity.
     * Ideally, the capacity will be at least the number of total elements * (1 / loadFactor)
     * to prevent future expansions.
     *
     * @param capacity
     */
    public AtomicIntIntMap(int capacity) {
        capacity = getActualCapacity(capacity);
        keys = makeEmptyArray(capacity);
        vals = new int[capacity];
        throw new IllegalStateException("THIS CLASS NEEDS TO BE TESTED!");
    }

    /**
     * Returns true iff the keys contains the specified key.
     * If the key is added before contains is called, it will always return true.
     * If the key is added while contains is called, it may or may not return true.
     * Will always return false if the map does not contain the key.
     *
     * @param key
     * @return
     */
    public boolean containsKey(int key) {
        return getIndex(keys, key) >= 0;
    }

    /**
     * Returns the index associated with the particular key, or -1 if it does not exist.
     * @param keys
     * @param key
     * @return
     */
    private int getIndex(AtomicIntegerArray keys, int key) {
        int length = keys.length();
        int hash = hash(key);
        int probe = 1 + (hash % (length - 2));
        int index = hash % length;
        int firstIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            int v = keys.get(index);
            if (v == unusedKey) {
                return -1;
            }  else if (v == key) {
                return index;
            }
        } while (index != firstIndex);

        return -1;
    }


    /**
     * Adds the specified value to the keys.
     * @param value
     */
    public void put(int key, int value) {
        if (key == unusedKey) {
            throw new IllegalArgumentException("Value " + key + " is used internally as an unused slot marker!");
        }

        numElements.incrementAndGet();
        expandIfNecessary();

        setInternal(keys, vals, key, value);
    }

    /**
     * Store a particular key and value in the arrays.
     * @param keys
     * @param vals
     * @param key
     * @param value
     */
    private void setInternal(AtomicIntegerArray keys, int[] vals, int key, int value) {
        // An implementation of Knuth's open addressing algorithm adapted from Trove's TLongHash.
        int length = keys.length();
        int hash = hash(value);
        int probe = 1 + (hash % (length - 2));
        int index = hash % length;
        int firstIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            long k = keys.get(index);
            if (k == key) {
                vals[index] = value;
                break; // already keys
            } else if (k == unusedKey && keys.compareAndSet(index, unusedKey, key)) {
                vals[index] = value;
                break;
            }
        } while (index != firstIndex);
    }

    /**
     * Returns the number of elements stored in the keys.
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
        if (numElements.get() < loadFactor * keys.length()) {
            return;
        }
        synchronized (numElements) {
            // Maybe somebody expanded while we were waiting for the lock
            if (numElements.get() < loadFactor * keys.length()) {
                return;
            }

            // expand, rehash
            int newSize = getActualCapacity((int) Math.ceil(keys.length() / loadFactor));
            AtomicIntegerArray newKeys = makeEmptyArray(newSize);
            int [] newVals = new int[newSize];
            for (int i = 0; i < keys.length(); i++) {
                int k = keys.get(i);
                if (k != unusedKey) {
                    setInternal(newKeys, newVals, k, vals[i]);
                }
            }
            keys = newKeys;
            vals = newVals;
        }
    }

    /**
     * Returns the keys.
     * This is a relatively expensive O(n) operation.
     * It will return all the elements in the keys at the start of the call, and
     * it may return any (or none of) the elements added while it is ongoing.
     *
     * @return the values in the keys.
     */
    public int[] keys() {
        TIntArrayList vals = new TIntArrayList();
        AtomicIntegerArray tmp = keys;      // could change out from under us...
        for (int i = 0; i < tmp.length(); i++) {
            int v = tmp.get(i);
            if (v != unusedKey) {
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
     * The array is filled with the unusedKey.
     *
     * @param capacity
     * @return
     */
    private AtomicIntegerArray makeEmptyArray(int capacity) {
        AtomicIntegerArray set = new AtomicIntegerArray(capacity);
        for (int i = 0; i < capacity; i++) {
            set.set(i, unusedKey);
        }
        return set;
    }

    private int getActualCapacity(int desiredCapacity) {
        int capacity = Math.max(desiredCapacity, 5);
        return PrimeFinder.nextPrime(capacity);
    }

    /**
     * Removes all elements in the keys.
     * Does not compact it.
     */
    public void clear() {
        AtomicIntegerArray tmp = keys;      // could change out from under us...
        for (int i = 0; i < tmp.length(); i++) {
            tmp.set(i, unusedKey);
        }
    }
}
