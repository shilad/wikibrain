package org.wikibrain.matrix;

import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

public class TestDenseMatrixRow {
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
            expected = Math.min(expected, DenseMatrixRow.MAX_SCORE);
            expected = Math.max(expected, DenseMatrixRow.MIN_SCORE);

            assertEquals(k, keys[i]);
            assertEquals(v, expected, 0.0001);
        }
    }

    public MatrixRow createRow() {
        LinkedHashMap<Integer, Float> m = new LinkedHashMap<Integer, Float>();
        assertEquals(keys.length, vals.length);
        for (int i = 0; i < keys.length; i++) {
            m.put(keys[i], vals[i]);
        }
        return new DenseMatrixRow(new ValueConf(), ROW_INDEX, m);
    }
}
