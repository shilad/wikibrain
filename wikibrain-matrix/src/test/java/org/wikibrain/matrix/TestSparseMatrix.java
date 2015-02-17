package org.wikibrain.matrix;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.*;

public class TestSparseMatrix {
    private List<SparseMatrixRow> srcRows;

    private int NUM_ROWS = 1000;
    private int MAX_COLS = NUM_ROWS * 2;
    private int MAX_KEY = Math.max(NUM_ROWS, MAX_COLS) * 10;

    @Before
    public void createTestData() throws IOException {
        srcRows = TestUtils.createSparseTestMatrixRows(NUM_ROWS, MAX_COLS, false);
    }

    @Test
    public void testWrite() throws IOException {
        File tmp = File.createTempFile("matrix", null);
        SparseMatrixWriter.write(tmp, srcRows.iterator());
    }
    @Test
    public void testReadWrite() throws IOException {
        File tmp = File.createTempFile("matrix", null);
        SparseMatrixWriter.write(tmp, srcRows.iterator());
        Matrix m1 = new SparseMatrix(tmp);
        Matrix m2 = new SparseMatrix(tmp);
    }

    @Test
    public void testExpandPageForHeader() throws IOException {
        List<SparseMatrixRow> shortRows = TestUtils.createSparseTestMatrixRows(1000, 100, false);
        File tmp = File.createTempFile("matrix", null);
        SparseMatrixWriter.write(tmp, shortRows.iterator());
        Matrix m1 = new SparseMatrix(tmp);
        assertEquals(1000, m1.getNumRows());
    }

    @Test
    public void testTranspose() throws IOException {
        for (int numOpenPages: new int[] { 1, Integer.MAX_VALUE}) {
            File tmp1 = File.createTempFile("matrix", null);
            File tmp2 = File.createTempFile("matrix", null);
            File tmp3 = File.createTempFile("matrix", null);
            SparseMatrixWriter.write(tmp1, srcRows.iterator());
            SparseMatrix m = new SparseMatrix(tmp1);
            verifyIsSourceMatrix(m);
            new SparseMatrixTransposer(m, tmp2, 1).transpose();
            SparseMatrix m2 = new SparseMatrix(tmp2);
            new SparseMatrixTransposer(m2, tmp3, 1).transpose();
            Matrix m3 = new SparseMatrix(tmp3);
            verifyIsSourceMatrixUnordered(m3, .001);
        }
    }


    @Test
    public void testRows() throws IOException {
        for (int numOpenPages: new int[] { 1, Integer.MAX_VALUE}) {
            File tmp = File.createTempFile("matrix", null);
            SparseMatrixWriter.write(tmp, srcRows.iterator());
            Matrix m = new SparseMatrix(tmp);
            verifyIsSourceMatrix(m);
        }
    }


    private void verifyIsSourceMatrix(Matrix m) throws IOException {
        assertEquals(srcRows.size(), m.getNumRows());
        int [] ids1 = m.getRowIds();
        int [] ids2 = new int[srcRows.size()];
        for (int i = 0; i < srcRows.size(); i++) {
            ids2[i] = srcRows.get(i).getRowIndex();
        }
        Arrays.sort(ids1);
        Arrays.sort(ids2);
        assertArrayEquals(ids2, ids1);

        for (SparseMatrixRow srcRow : srcRows) {
            MatrixRow destRow = m.getRow(srcRow.getRowIndex());
            assertNotNull(destRow);
            assertEquals(destRow.getRowIndex(), srcRow.getRowIndex());
            assertEquals(destRow.getNumCols(), srcRow.getNumCols());
            for (int i = 0; i < destRow.getNumCols(); i++) {
                assertEquals(srcRow.getColIndex(i), destRow.getColIndex(i));
                assertEquals(srcRow.getColValue(i), destRow.getColValue(i), 0.01);
            }
        }
    }

    private void verifyIsSourceMatrixUnordered(Matrix m, double delta) throws IOException {
        for (SparseMatrixRow srcRow : srcRows) {
            MatrixRow destRow = m.getRow(srcRow.getRowIndex());
            LinkedHashMap<Integer, Float> destRowMap = destRow.asMap();
            assertEquals(destRow.getRowIndex(), srcRow.getRowIndex());
            assertEquals(destRow.getNumCols(), srcRow.getNumCols());
            for (int i = 0; i < srcRow.getNumCols(); i++) {
                int colId = srcRow.getColIndex(i);
                assertTrue(destRowMap.containsKey(colId));
                assertEquals(srcRow.getColValue(i), destRowMap.get(colId), delta);
            }
        }
    }
}
