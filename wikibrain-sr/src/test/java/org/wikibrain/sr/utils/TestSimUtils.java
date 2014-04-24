package org.wikibrain.sr.utils;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestSimUtils {
    public static final int[] keyList1 = {-5, 10, 3000, 504};
    public static final int[] keyList2 = {504, 29, -8, 10, -300, 203, 177};
    public static final double[] valueList = {1.8, -3.3, 14.222, 0.0, 4.7753, 3.49, -6.75, 67, -2.88};

    public TIntDoubleHashMap zeroVector(int[] keyList) {
        TIntDoubleHashMap v = new TIntDoubleHashMap();
        for (int keyNum : keyList) {
            v.put(keyNum, 0.0);
        }
        return v;
    }

    public TIntDoubleHashMap testVector(int[] keyList, int i) {
        TIntDoubleHashMap v = new TIntDoubleHashMap();
        for (int keyNum : keyList) {
            int valueNum = Math.abs((keyNum + i) % valueList.length);
            v.put(keyNum, valueList[valueNum]);
        }
        return v;
    }



    @Test
    public void testCosineSimilarity() {
        TIntDoubleHashMap zeroVector = zeroVector(keyList1);
        TIntDoubleHashMap testVector1 = testVector(keyList2, 0);
        TIntDoubleHashMap testVector2 = testVector(keyList2, 1);
        assertEquals("Cosine similarity between a vector and itself must be 1",
                1.0, SimUtils.cosineSimilarity(testVector1, testVector1), 0.0);
        assertEquals("Cosine similarity between a vector and zero vector must be 0",
                0.0, SimUtils.cosineSimilarity(testVector1, zeroVector), 0.0);
    }

    @Test
    public void testNormalizeVector() {
        TIntDoubleHashMap zeroVector1 = zeroVector(keyList1);
        TIntDoubleHashMap testVector1 = testVector(keyList2, 0);
        TIntDoubleHashMap testVector2 = testVector(keyList2, 1);
        TIntDoubleMap zeroVector1Normalized = SimUtils.normalizeVector(zeroVector1);
        TIntDoubleMap testVector1Normalized = SimUtils.normalizeVector(testVector1);
        TIntDoubleMap testVector2Normalized = SimUtils.normalizeVector(testVector2);
        // Normalize a zero vector returns the original zero vector
        for (int keyNum : zeroVector1.keys()) {
            assertEquals("Every score in the zero vector remains the same after normalization",
                    zeroVector1.get(keyNum), zeroVector1Normalized.get(keyNum), 0.0);
        }
        double testValue = 0.0;
        for (double value : testVector1Normalized.values()) {
            testValue += value * value;
        }
        assertEquals("Normalized vector has length of 1",
                1.0, testValue, 0.00001);
    }

    @Test
    public void testSortByValue() {
        int testMapSize = 1000;
        Random random = new Random(System.currentTimeMillis());
        TIntDoubleHashMap testMap = new TIntDoubleHashMap();
        for(int i = 0 ; i < testMapSize ; ++i) {
            testMap.put(random.nextInt(), random.nextDouble());
        }
        Map<Integer, Double> sortedMap = SimUtils.sortByValue(testMap);
        Assert.assertEquals(testMapSize, sortedMap.size() );
        Double previous = null;
//        for(Map.Entry<Integer, Double> entry : sortedMap.entrySet()) {
//            Assert.assertNotNull( entry.getScore() );
//            if (previous != null) {
//                Assert.assertTrue( entry.getScore() >= previous );
//            }
//            previous = entry.getScore();
//        }
    }

}
