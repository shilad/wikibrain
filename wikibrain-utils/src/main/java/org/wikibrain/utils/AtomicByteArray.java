package org.wikibrain.utils;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @author Shilad Sen
 */
public class AtomicByteArray {
    private final AtomicIntegerArray array;
    private final int length;

    public AtomicByteArray(final int length) {
        this.length = length;
        this.array = new AtomicIntegerArray((length + 3) / 4);
    }

    public void set(final int i, byte val) {
        int idx = i >>> 2;
        int shift = (i & 3) << 3;
        int mask = 0xFF << shift;
        int val2 = (val & 0xff) << shift;

        while (true) {
            final int num = this.array.get(idx);
            final int num2 = (num & ~mask) | val2;
            if ((num == num2) || this.array.compareAndSet(idx, num, num2)) {
                return;
            }
        }
    }

    public boolean compareAndSet(final int i, final byte expected, final byte val) {
        int idx = i >>> 2;
        int shift = (i & 3) << 3;
        int mask = 0xFF << shift;
        int expected2 = (expected & 0xff) << shift;
        int val2 = (val & 0xff) << shift;

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

    public byte get(final int i) {
        return (byte) (this.array.get(i >>> 2) >> ((i & 3) << 3));
    }

    public int length() {
        return this.length;
    }
}
