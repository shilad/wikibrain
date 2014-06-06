package org.wikibrain.matrix;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class BenchBuffers {
    public static final int NUM_INTS = 4000000;
    public static final int NUM_ITERATIONS = 100;

    public static void main(String args[]) throws IOException {
        File tmpFile = File.createTempFile("tmp", "bytes");
        tmpFile.deleteOnExit();
        Random random = new Random();
        int ints[] = new int[NUM_INTS];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = random.nextInt();
        }
        Arrays.sort(ints);

        ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(ints);

        FileUtils.writeByteArrayToFile(tmpFile, byteBuffer.array());

        for (int i = 0; i < 10; i++) {
            timeArray(ints);
        }
        for (int i = 0; i < 10; i++) {
            timeBuffer(tmpFile);
        }
        for (int i = 0; i < 10; i++) {
            timeHashSearches(ints);
        }
        for (int i = 0; i < 10; i++) {
            timeBufferSearches(tmpFile);
        }
        for (int i = 0; i < 10; i++) {
            timeBinarySearches(ints);
        }
        tmpFile.delete();
    }

    public static void timeArray(int[] ints) {
        long before = System.currentTimeMillis();
        long total = 0;
        for (int t = 0; t < NUM_ITERATIONS; t++) {
            for (int i = 0; i < ints.length; i++) {
                if (ints[i] > Integer.MIN_VALUE) {
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
        IntBuffer ints = buffer.asIntBuffer();
        long before = System.currentTimeMillis();
        long total = 0;
        for (int t = 0; t < NUM_ITERATIONS; t++) {
            for (int i = 0; i < ints.capacity(); i++) {
                if (ints.get(i) > Integer.MIN_VALUE) {
                    total++;
                }
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("buffer ops per milli is " + 1.0 * total / (after - before));
    }
    public static void timeBinarySearches(int [] ints) {
        long before = System.currentTimeMillis();
        long hits = 0;
        for (int i = 0; i < NUM_INTS; i++) {
            int goal = ints[Math.abs(i * 3571) % ints.length];
            int lo = 0;
            int hi = ints.length - 1;
            while (lo <= hi) {
                int mid = (lo + hi) / 2;
                int midId = ints[mid];
                if (goal < midId) {
                    hi = mid - 1;
                } else if (goal > midId) {
                    lo = mid + 1;
                } else {
                    hits++;
                    break;
                }
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("array searchers per milli is " +
                1.0 * (NUM_INTS) / (after - before) +
                " with " + hits + " hits");
    }

    public static void timeBufferSearches(File file) throws IOException {
        FileChannel channel = (new FileInputStream(file)).getChannel();
        long size = channel.size();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        IntBuffer ints = buffer.asIntBuffer();
        long before = System.currentTimeMillis();
        long hits = 0;
        int n = ints.capacity();
        for (int i = 0; i < NUM_INTS; i++) {
            int goal = ints.get(Math.abs(i * 3571) % n);
            int lo = 0;
            int hi = n - 1;
            while (lo <= hi) {
                int mid = (lo + hi) / 2;
                int midId = ints.get(mid);
                if (goal < midId) {
                    hi = mid - 1;
                } else if (goal > midId) {
                    lo = mid + 1;
                } else {
                    hits++;
                    break;
                }
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("buffer searchers per milli is " +
                1.0 * (NUM_INTS) / (after - before) +
                " with " + hits + " hits");
    }


    private static void timeHashSearches(int[] ints) {
        TIntIntMap indexes = new TIntIntHashMap(ints.length);
        for (int i = 0; i < ints.length; i++) {
            indexes.put(ints[i], i);
        }
        long before = System.currentTimeMillis();
        long hits = 0;

        for (int i = 0; i < NUM_INTS; i++) {
            int goal = ints[Math.abs(i * 3571) % ints.length];
            if (indexes.get(goal) > 0) {
                hits++;
            }
        }
        long after = System.currentTimeMillis();
        System.err.println("hash searchers per milli is " +
                1.0 * (NUM_INTS) / (after - before) +
                " with " + hits + " hits");
    }

}
