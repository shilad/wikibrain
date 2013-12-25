package org.wikapidia.matrix;


import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestMmapMemoryLeak {
    static final int NUM_ROWS = 10000;
    static final int MAX_ROW_LENGTH = 1000;

    static final File TMP_MATRIX_FILE = new File("./matrix.tmp");


    @Test
    public void testOnlyOnePageLoaded() throws IOException {
        SparseMatrix m = TestUtils.createSparseTestMatrix(NUM_ROWS, MAX_ROW_LENGTH, false, NUM_ROWS * 20, false);
        MatrixRow first = m.getRow(m.getRowIds()[0]); // force the first page to load.
        first = null;
        MappedByteBuffer buffer = m.rowBuffers.buffers.get(0).buffer;
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(buffer);
        assertNotNull(buffer);
        long t = System.currentTimeMillis();
        for (int j = 0; j < 100; j++) {
        int i = 0;
        for (MatrixRow row : m) {
            i++;
        }
            assertEquals(i, NUM_ROWS);
        }
        System.err.println(System.currentTimeMillis() - t);
        buffer = m.rowBuffers.buffers.get(0).buffer;
        assertNull(buffer); // only the last page should be loaded
        verifier.assertGarbageCollected("mapped byte buffer");
    }

    @Ignore
    @Test
    public void testBigMatrix() throws IOException, InterruptedException {
        if (!TMP_MATRIX_FILE.isFile()) {
            SparseMatrix m = TestUtils.createSparseTestMatrix(100000, 1000, false, 1024*1024, false);
            m.close();
            m.getPath().renameTo(TMP_MATRIX_FILE);
        }

        List<Thread> threads = new ArrayList<Thread>();
        final SparseMatrix m = new SparseMatrix(TMP_MATRIX_FILE, 3, 1024*1024);
        for (int i = 0; i < 8; i++) {
            final int finalI = i;
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        test(finalI, m);
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            };
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    private void test(int threadNum, SparseMatrix m) throws IOException {
        int rowIds[] = m.getRowIds();
        Random random = new Random();
        long counter = 0;
        long sumIds = 0;
        double sumScores = 0;
        while (true) {
            int id = rowIds[random.nextInt(rowIds.length)];
            SparseMatrixRow row = m.getRow(id);
            for (int i = 0; i < row.getNumCols(); i++) {
                sumIds += row.getColIndex(i);
                sumScores += row.getColValue(i);
            }
            if (counter++ % 10000 == 0) {
                System.err.println(threadNum + " doing: " + counter + ", " + sumIds + ", " + sumScores);
            }
        }
    }
}
