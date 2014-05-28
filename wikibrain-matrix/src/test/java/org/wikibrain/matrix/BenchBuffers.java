package org.wikibrain.matrix;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class BenchBuffers {
    public static final int NUM_BYTES = 100000000;
    public static final int NUM_ITERATIONS = 10;

    public static void main(String args[]) throws IOException {
        File tmpFile = File.createTempFile("tmp", "bytes");
        tmpFile.deleteOnExit();
        byte[] bytes = new byte[NUM_BYTES];
        Random random = new Random();
        random.nextBytes(bytes);
        FileUtils.writeByteArrayToFile(tmpFile, bytes);

        for (int i = 0; i < 10; i++) {
            timeArray(bytes);
        }
        for (int i = 0; i < 10; i++) {
            timeBuffer(tmpFile);
        }
    }
    public static void timeArray(byte[] bytes) {
        long before = System.currentTimeMillis();
        long total = 0;
        for (int t = 0; t < NUM_ITERATIONS; t++) {
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] >= Byte.MIN_VALUE + 1) {
                    total++;
                }
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("array ops per milli is " + 1.0 * total / (after - before));
    }

    public static void timeBuffer(File file) throws IOException {
        FileChannel channel = (new FileInputStream(file)).getChannel();
        long size = channel.size();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        long before = System.currentTimeMillis();
        long total = 0;
        for (int t = 0; t < NUM_ITERATIONS; t++) {
            for (int i = 0; i < size; i++) {
                if (buffer.get(i) >= Byte.MIN_VALUE + 1) {
                    total++;
                }
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("buffer ops per milli is " + 1.0 * total / (after - before));
    }

    public static void timeByteBuffer(File file) throws IOException {
        FileChannel channel = (new FileInputStream(file)).getChannel();
        long size = channel.size();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        long before = System.currentTimeMillis();
        long total = 0;
        for (int t = 0; t < NUM_ITERATIONS; t++) {
            for (int i = 0; i < size; i++) {
                if (buffer.get(i) >= Byte.MIN_VALUE + 1) {
                    total++;
                }
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("buffer ops per milli is " + 1.0 * total / (after - before));
    }
}
