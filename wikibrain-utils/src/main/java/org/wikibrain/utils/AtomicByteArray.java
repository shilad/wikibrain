package org.wikibrain.utils;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 *
 *
 * A {@code byte} array in which elements may be updated atomically.
 * See the {@link java.util.concurrent.atomic} package
 * specification for description of the properties of atomic
 * variables.
 *
 * This is an adaptation of Java's AtomicIntegerArray for bytes.
 *
 * Four bytes are packed into each int.
 * Only a few methods are implemented for now.
 *
 * @author Shilad Sen
 */
public class AtomicByteArray {
    private final AtomicIntegerArray array;
    private final int length;

    /**
     * Creates a new AtomicByteArray of the given length, with all
     * elements initially zero.
     *
     * @param length the length of the array
     */
    public AtomicByteArray(final int length) {
        this.length = length;
        this.array = new AtomicIntegerArray((length + 3) / 4);
    }

    /**
     * Sets the element at position {@code i} to the given value.
     *
     * @param i the index
     * @param newValue the new value
     */
    public void set(int i, byte newValue) {
        int idx = i >>> 2;
        int shift = (i & 3) << 3;
        int mask = 0xFF << shift;
        int val2 = (newValue & 0xff) << shift;

        while (true) {
            final int num = this.array.get(idx);
            final int num2 = (num & ~mask) | val2;
            if ((num == num2) || this.array.compareAndSet(idx, num, num2)) {
                return;
            }
        }
    }

    /**
     * Atomically sets the element at position {@code i} to the given
     * updated value if the current value {@code ==} the expected value.
     *
     * @param i the index
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public boolean compareAndSet(int i, byte expect, byte update) {
        int idx = i >>> 2;
        int shift = (i & 3) << 3;
        int mask = 0xFF << shift;
        int expected2 = (expect & 0xff) << shift;
        int val2 = (update & 0xff) << shift;

        while (true) {
            final int num = this.array.get(idx);
            // Check that the read byte is what we expected
            if ((num & mask) != expected2) return false;

            // If we complete successfully, all is good
            final int num2 = (num & ~mask) | val2;
            if ((num == num2) || this.array.compareAndSet(idx, num, num2)) {
                return true;
            }
        }
    }


    /**
     * Atomically increments by one the element at index {@code i}.
     *
     * @param i the index
     * @return the previous value
     */
    public final byte getAndIncrement(int i) {
        return getAndAdd(i, 1);
    }

    /**
     * Atomically decrements by one the element at index {@code i}.
     *
     * @param i the index
     * @return the previous value
     */
    public final byte getAndDecrement(int i) {
        return getAndAdd(i, -1);
    }

    /**
     * Atomically adds the given value to the element at index {@code i}.
     *
     * @param i the index
     * @param delta the value to add
     * @return the previous value
     */
    public final byte getAndAdd(int i, int delta) {
        while (true) {
            byte current = get(i);
            byte next = (byte) (current + delta);
            if (compareAndSet(i, current, next))
                return current;
        }
    }

    /**
     * Atomically increments by one the element at index {@code i}.
     *
     * @param i the index
     * @return the updated value
     */
    public final byte incrementAndGet(int i) {
        return addAndGet(i, 1);
    }

    /**
     * Atomically decrements by one the element at index {@code i}.
     *
     * @param i the index
     * @return the updated value
     */
    public final byte decrementAndGet(int i) {
        return addAndGet(i, -1);
    }

    /**
     * Atomically adds the given value to the element at index {@code i}.
     *
     * @param i the index
     * @param delta the value to add
     * @return the updated value
     */
    public final byte addAndGet(int i, int delta) {
        while (true) {
            byte current = get(i);
            byte next = (byte) (current + delta);
            if (compareAndSet(i, current, next))
                return next;
        }
    }

    /**
     * Gets the current value at position {@code i}.
     *
     * @param i the index
     * @return the current value
     */
    public byte get(final int i) {
        return (byte) (this.array.get(i >>> 2) >> ((i & 3) << 3));
    }

    /**
     * Returns the length of the array.
     *
     * @return the length of the array
     */
    public int length() {
        return this.length;
    }
}
