//package org.wikibrain.sr;
//
//import gnu.trove.map.hash.TIntDoubleHashMap;
//import gnu.trove.map.hash.TLongDoubleHashMap;
//import org.apache.commons.collections.CollectionUtils;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.wikibrain.matrix.*;
//import org.wikibrain.sr.pairwise.PairwiseCosineSimilarity;
//import org.wikibrain.sr.pairwise.PairwiseMilneWittenSimilarity;
//import org.wikibrain.sr.pairwise.PairwiseSimilarity;
//import org.wikibrain.sr.pairwise.PairwiseSimilarityWriter;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.Assert.assertEquals;
//
///**
//* @author Ben Hillmann
//* @author Shilad Sen
//*/
//
//public class TestPairwiseSimilarity {
//    static int NUM_ROWS = 100;
//
//    private static SparseMatrix matrix;
//    private static SparseMatrix transpose;
//
//    @BeforeClass
//    public static void createTestData() throws IOException {// Create test data and transpose
//
//        matrix = TestUtils.createSparseTestMatrix(NUM_ROWS, NUM_ROWS, false);
//        SparseMatrixWriter sparseMatrixWriter = new SparseMatrixWriter(new File("matrix-feature"), new ValueConf());
//        for (SparseMatrixRow row: matrix) {
//            sparseMatrixWriter.writeRow(row);
//        }
//        sparseMatrixWriter.finish();
//        new SparseMatrixTransposer(matrix, new File("matrix-transpose"), 10).transpose();
//    }
//
//    @Ignore
//    @Test
//    public void testSimilarity() throws IOException, InterruptedException {
//
//        String path = "matrix";
//        PairwiseSimilarity pairwiseSimilarity = new PairwiseMilneWittenSimilarity(path);
//        PairwiseSimilarityWriter writer = new PairwiseSimilarityWriter(path,pairwiseSimilarity);
//        writer.writeSims(matrix.getRowIds(), 1, NUM_ROWS);
//        SparseMatrix sims = new SparseMatrix(new File("matrix-cosimilarity"));
//
//        // Calculate similarities by hand
//        TLongDoubleHashMap dot = new TLongDoubleHashMap();
//        TIntDoubleHashMap len2 = new TIntDoubleHashMap();
//
//        for (SparseMatrixRow row1 : matrix) {
//            Map<Integer, Float> data1 = row1.asMap();
//            int id1 = row1.getRowIndex();
//
//            // Calculate the length^2
//            double len = 0.0;
//            for (double val : data1.values()) {
//                len += val * val;
//            }
//            len2.put(id1, len);
//
//            for (SparseMatrixRow row2 : matrix) {
//                int id2 = row2.getRowIndex();
//                Map<Integer, Float> data2 = row2.asMap();
//                double sim = 0.0;
//
//                for (Object key : CollectionUtils.intersection(data1.keySet(), data2.keySet())) {
//                    sim += data1.get(key) * data2.get(key);
//                }
//                if (sim != 0) {
//                    dot.put(pack(id1, id2), sim);
//                }
//            }
//        }
//
//        int numCells = 0;
//        for (MatrixRow row : sims) {
//            for (int i = 0; i < row.getNumCols(); i++) {
//                if (row.getColValue(i) != 0) {
//                    int id1 = row.getRowIndex();
//                    int id2 = row.getColIndex(i);
//                    numCells++;
//                    double xDotX = len2.get(id1);
//                    double yDotY = len2.get(id2);
//                    double xDotY = dot.get(pack(id1, id2));
//
//                    assertEquals(row.getColValue(i), xDotY / Math.sqrt(xDotX * yDotY), 0.001);
//                }
//            }
//        }
//        assertEquals(numCells, dot.size());
//    }
//
//    @Test
//    public void testSimilarityMatchesMostSimilar() throws IOException {
//        PairwiseCosineSimilarity cosine = new PairwiseCosineSimilarity("matrix");
//        int[] ids = matrix.getRowIds();
//        Map<Integer, TIntDoubleHashMap> sims = new HashMap<Integer, TIntDoubleHashMap>();
//        for (int id : ids) {
//            sims.put(id, new TIntDoubleHashMap());
//            for (SRResult score : cosine.mostSimilar(id, NUM_ROWS, null)) {
//                sims.get(id).put(score.getId(), score.getScore());
//            }
//        }
//        for (int id1 : ids) {
//            for (int id2 : ids) {
//                double s = cosine.similarity(id1, id2);
//                if (sims.containsKey(id1) && sims.get(id1).containsKey(id2)) {
//                    assertEquals(s, sims.get(id1).get(id2), 0.001);
//                } else {
//                    assertEquals(s, 0.0, 0.0001);
//                }
//            }
//        }
//    }
//
//    private long pack(int x, int y) {
//        return ByteBuffer.wrap(new byte[8]).putInt(x).putInt(y).getLong(0);
//
//}
//        }
