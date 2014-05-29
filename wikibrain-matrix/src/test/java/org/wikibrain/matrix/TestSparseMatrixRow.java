package org.wikibrain.matrix;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestSparseMatrixRow {
    private int[] keys = new int[] { 3, 9, 11, 26, 54 };
    private float[] vals = new float[] {1.0f, 0.7f, 2.0f, 0.1f, -0.1f};
    private int ROW_INDEX = 34;

    @Test
    public void testWrite() {
        MatrixRow row = createRow();
        assertEquals(row.getRowIndex(), ROW_INDEX);
        assertEquals(row.getNumCols(), keys.length);
        for (int i = 0; i < keys.length; i++) {
            int k = row.getColIndex(i);
            float v = row.getColValue(i);
            float expected = vals[i];

            // pinch it
            expected = Math.min(expected, SparseMatrixRow.MAX_SCORE);
            expected = Math.max(expected, SparseMatrixRow.MIN_SCORE);

            assertEquals(k, keys[i]);
            assertEquals(v, expected, 0.0001);
        }
    }

    @Test
    public void testSorting() {
        int maxColumns = 100000;
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            TIntFloatMap vector = new TIntFloatHashMap();
            int n = random.nextInt(maxColumns);
            for (int c = 0; c < n; c++) {
                vector.put(random.nextInt(), random.nextFloat());
            }
            int keys[] = vector.keys();
            float vals[] = vector.values();

            SparseMatrixRow row = new SparseMatrixRow(new ValueConf(), ROW_INDEX, keys, vals);
            assertEquals(keys.length, row.getNumCols());

            Arrays.sort(keys);
            for (int j = 0; j < row.getNumCols(); j++) {
                int k = row.getColIndex(j);
                float v = row.getColValue(j);
                assertEquals(k, keys[j]);
                assertTrue(vector.containsKey(k));
                assertEquals(vector.get(k), v, 0.01);
            }
        }
    }

    public MatrixRow createRow() {
        LinkedHashMap<Integer, Float> m = new LinkedHashMap<Integer, Float>();
        assertEquals(keys.length, vals.length);
        for (int i = 0; i < keys.length; i++) {
            m.put(keys[i], vals[i]);
        }
        return new SparseMatrixRow(new ValueConf(), ROW_INDEX, m);
    }
}
