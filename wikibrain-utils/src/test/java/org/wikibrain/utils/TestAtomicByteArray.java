package org.wikibrain.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestAtomicByteArray {
    @Test
    public void testSimple() {
        AtomicByteArray a = new AtomicByteArray(100);
        for (int i = 0; i < 1000; i++) {
            // clear the array
            for (int j = 0; j < a.length(); j++) {
                a.set(j, (byte) 0);
            }
            for (int j = 0; j < a.length(); j++) {
                assertEquals(0, ((int)a.get(j)));
            }
            // test set and get
            for (int j = 0; j < a.length(); j++) {
                a.set(j, (byte) (Byte.MIN_VALUE + (j * i) % 256));
            }
            for (int j = 0; j < a.length(); j++) {
                assertEquals(Byte.MIN_VALUE + (j * i) % 256, (int)a.get(j));
            }
            // set the array to index
            for (int j = 0; j < a.length(); j++) {
                a.set(j, (byte) j);
            }
            for (int j = 0; j < a.length(); j++) {
                assertEquals((byte)j, a.get(j));
            }
            // test CAS and get
            for (int j = 0; j < a.length(); j++) {
                a.compareAndSet(j, (byte) j, (byte) (Byte.MIN_VALUE + (j * i) % 256));
            }
            for (int j = 0; j < a.length(); j++) {
                assertEquals(Byte.MIN_VALUE + (j * i) % 256, (int) a.get(j));
            }
        }
    }
}
