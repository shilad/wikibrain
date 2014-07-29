package org.wikibrain.sr.vector;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.matrix.SparseMatrixRow;
import org.wikibrain.matrix.ValueConf;
import org.wikibrain.sr.utils.SimUtils;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestCosineSimilarity {
    static final int   [] ROW1_IDS   = {    7,    9,   12,    5,    6,    2 };
    static final float [] ROW1_VALS  = { 0.3f, 0.5f, 0.2f, 0.7f, 0.8f, 0.1f };

    static final int   [] ROW2_IDS  = {    5,    3,    2,    4,    7 };
    static final float [] ROW2_VALS = { 0.8f, 0.1f, 0.2f, 0.4f, 0.5f };

    @Test
    public void testUtil() {
        TIntFloatMap row1 = getMap(ROW1_IDS, ROW1_VALS);
        TIntFloatMap row2 = getMap(ROW2_IDS, ROW2_VALS);
        double expected = cosineSimilarity(row1, row2);
        double actual = SimUtils.cosineSimilarity(row1, row2);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    public void testMap() {
        TIntFloatMap row1 = getMap(ROW1_IDS, ROW1_VALS);
        TIntFloatMap row2 = getMap(ROW2_IDS, ROW2_VALS);
        double expected = cosineSimilarity(row1, row2);
        double actual = new CosineSimilarity().similarity(row1, row2);
        assertEquals(expected, actual, 0.0001);
        actual = new CosineSimilarity().similarity(row2, row1);
        assertEquals(expected, actual, 0.0001);
    }

    @Test
    public void testRows() {
        TIntFloatMap map1 = getMap(ROW1_IDS, ROW1_VALS);
        TIntFloatMap map2 = getMap(ROW2_IDS, ROW2_VALS);
        SparseMatrixRow row1 = getRow(ROW1_IDS, ROW1_VALS);
        SparseMatrixRow row2 = getRow(ROW2_IDS, ROW2_VALS);
        double expected = cosineSimilarity(map1, map2);
        double actual = new CosineSimilarity().similarity(row1, row2);
        assertEquals(expected, actual, 0.0001);
        actual = new CosineSimilarity().similarity(row2, row1);
        assertEquals(expected, actual, 0.0001);
    }

    private double cosineSimilarity(TIntFloatMap row1, TIntFloatMap row2) {
        double adota = 0.0;
        double bdotb = 0.0;
        double adotb = 0.0;

        for (double v : row1.values()) {
            adota += v * v;
        }
        for (double v : row2.values()) {
            bdotb += v * v;
        }
        for (int id : row1.keys()) {
            if (row2.containsKey(id)) {
                adotb += row1.get(id) * row2.get(id);
            }
        }
        return adotb / (Math.sqrt(adota) * Math.sqrt(bdotb));
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

    @Ignore
    @Test
    public void benchmark() {
        int numOuter = 100;
        int numInner = 10000;

        long before = System.currentTimeMillis();
        double sum = 0;

        Random random = new Random();
        for (int i = 0; i < numOuter; i++) {
            int overlap[] = new int[10];
            for (int j = 0; j < overlap.length; j++) {
                overlap[j] = random.nextInt(Integer.MAX_VALUE / 10);
            }

            SparseMatrixRow row1 = makeRow(100, overlap);
            SparseMatrixRow row2 = makeRow(100, overlap);
            CosineSimilarity sim = new CosineSimilarity();
            for (int j = 0; j < numInner; j++) {
                sum += sim.similarity(row1, row2);
            }
        }
        long after = System.currentTimeMillis();
        System.out.println("elapsed is " + (after - before) + " sim is " + (sum / (numOuter * numInner)));
    }

    private SparseMatrixRow makeRow(int size, int[] mustInclude) {
        Random random = new Random();
        int ids[] = new int[size];
        float vals[] = new float[size];
        for (int i = 0; i < size; i++) {
            ids[i] = (i < mustInclude.length) ? mustInclude[i] : random.nextInt(Integer.MAX_VALUE / 10);
            vals[i] = random.nextFloat();
        }
        return getRow(ids, vals);
    }
}
