package org.wikapidia.matrix;

import gnu.trove.map.TIntDoubleMap;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSparseMatrix {
    private List<TIntDoubleMap> srcRows;

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
        Matrix m1 = new SparseMatrix(tmp, Integer.MAX_VALUE, NUM_ROWS*20);
        Matrix m2 = new SparseMatrix(tmp, 1, NUM_ROWS*20);
    }

    @Test
    public void testTranspose() throws IOException {
        for (int numOpenPages: new int[] { 1, Integer.MAX_VALUE}) {
            File tmp1 = File.createTempFile("matrix", null);
            File tmp2 = File.createTempFile("matrix", null);
            File tmp3 = File.createTempFile("matrix", null);
            SparseMatrixWriter.write(tmp1, srcRows.iterator());
            SparseMatrix m = new SparseMatrix(tmp1, numOpenPages, MAX_KEY * 50);
            verifyIsSourceMatrix(m);
            new SparseMatrixTransposer(m, tmp2, 1).transpose();
            SparseMatrix m2 = new SparseMatrix(tmp2, numOpenPages, MAX_KEY * 50);
            new SparseMatrixTransposer(m2, tmp3, 1).transpose();
            Matrix m3 = new SparseMatrix(tmp3, numOpenPages, MAX_KEY * 50);
            verifyIsSourceMatrixUnordered(m3, .001);
        }
    }


    @Test
    public void testRows() throws IOException {
        for (int numOpenPages: new int[] { 1, Integer.MAX_VALUE}) {
            File tmp = File.createTempFile("matrix", null);
            SparseMatrixWriter.write(tmp, srcRows.iterator());
            Matrix m = new SparseMatrix(tmp, numOpenPages, NUM_ROWS * 20);
            verifyIsSourceMatrix(m);
        }
    }


    private void verifyIsSourceMatrix(Matrix m) throws IOException {
        for (TIntDoubleMap srcRow : srcRows) {
            MatrixRow destRow = m.getRow(srcRow.getRowIndex());
            assertEquals(destRow.getRowIndex(), srcRow.getRowIndex());
            assertEquals(destRow.getNumCols(), srcRow.getNumCols());
            for (int i = 0; i < destRow.getNumCols(); i++) {
                assertEquals(srcRow.getColIndex(i), destRow.getColIndex(i));
                assertEquals(srcRow.getColValue(i), destRow.getColValue(i), 0.01);
            }
        }
    }

    private void verifyIsSourceMatrixUnordered(Matrix m, double delta) throws IOException {
        for (TIntDoubleMap srcRow : srcRows) {
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
