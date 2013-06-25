package org.wikapidia.matrix;


import org.junit.Test;

import java.io.IOException;
import java.nio.MappedByteBuffer;

import static org.junit.Assert.*;

public class TestMmapMemoryLeak {
    static final int NUM_ROWS = 10000;
    static final int MAX_ROW_LENGTH = 1000;

    @Test
    public void testOnlyOnePageLoaded() throws IOException {
        SparseMatrix m = TestUtils.createSparseTestMatrix(NUM_ROWS, MAX_ROW_LENGTH, false, NUM_ROWS * 20, false);
        MatrixRow first = m.getRow(m.getRowIds()[0]); // force the first page to load.
        first = null;
        MappedByteBuffer buffer = m.rowBuffers.buffers.get(0).buffer;
        MemoryLeakVerifier verifier = new MemoryLeakVerifier(buffer);
        assertNotNull(buffer);
        int i = 0;
        for (MatrixRow row : m) {
            i++;
        }
        assertEquals(i, NUM_ROWS);
        buffer = m.rowBuffers.buffers.get(0).buffer;
        assertNull(buffer); // only the last page should be loaded
        verifier.assertGarbageCollected("mapped byte buffer");
    }
}
