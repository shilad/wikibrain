package org.wikibrain.sr.vector;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.junit.Test;
import org.wikibrain.matrix.SparseMatrixRow;
import org.wikibrain.matrix.ValueConf;
import org.wikibrain.sr.utils.SimUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class TestGoogleSimilarity {
    private static final int NUM_PAGES = 100;

    static final int   [] ROW1_IDS   = {    7,    9,   12,    5,    6,    2 };
    static final float [] ROW1_VALS  = { 0.3f, 0.5f, 0.2f, 0.7f, 0.8f, 0.1f };

    static final int   [] ROW2_IDS  = {    5,    3,    2,    4,    7 };
    static final float [] ROW2_VALS = { 0.8f, 0.1f, 0.2f, 0.4f, 0.5f };

    @Test
    public void testUtils() {
        TIntFloatMap row1 = getMap(ROW1_IDS, ROW1_VALS);
        TIntFloatMap row2 = getMap(ROW2_IDS, ROW2_VALS);
        double expected = googleSimilarity(row1, row2);
        double actual = SimUtils.googleSimilarity(6, 5, 3, NUM_PAGES);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    public void testMap() {
        TIntFloatMap row1 = getMap(ROW1_IDS, ROW1_VALS);
        TIntFloatMap row2 = getMap(ROW2_IDS, ROW2_VALS);
        double expected = googleSimilarity(row1, row2);
        double actual = new GoogleSimilarity(NUM_PAGES).similarity(row1, row2);
        assertEquals(expected, actual, 0.0001);
        actual = new GoogleSimilarity(NUM_PAGES).similarity(row2, row1);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    public void testRows() {
        TIntFloatMap map1 = getMap(ROW1_IDS, ROW1_VALS);
        TIntFloatMap map2 = getMap(ROW2_IDS, ROW2_VALS);
        SparseMatrixRow row1 = getRow(ROW1_IDS, ROW1_VALS);
        SparseMatrixRow row2 = getRow(ROW2_IDS, ROW2_VALS);
        double expected = googleSimilarity(map1, map2);
        double actual = new GoogleSimilarity(NUM_PAGES).similarity(row1, row2);
        assertEquals(expected, actual, 0.0001);
        actual = new GoogleSimilarity(NUM_PAGES).similarity(row2, row1);
        assertEquals(expected, actual, 0.0001);
    }

    private double googleSimilarity(TIntFloatMap row1, TIntFloatMap row2) {
        int na = row1.size();
        int nb = row2.size();
        int intersection = 0;

        for (int id : row1.keys()) {
            if (row2.containsKey(id)) {
                intersection++;
            }
        }
        return 1.0 - (Math.log(Math.max(na, nb)) - Math.log(intersection)) / (Math.log(NUM_PAGES) - Math.log(Math.min(na, nb)));
    }

    private SparseMatrixRow getRow(int [] ids, float [] vals) {
        assertEquals(ids.length, vals.length);
        return new SparseMatrixRow(new ValueConf(), 34, ids, vals);
    }

    private TIntFloatMap getMap(int []ids, float [] vals) {
        assertEquals(ids.length, vals.length);
        TIntFloatHashMap map = new TIntFloatHashMap();
        for (int i = 0; i < ids.length; i++) {
            map.put(ids[i], vals[i]);
        }
        return map;
    }
}
