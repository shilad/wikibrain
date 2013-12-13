package org.wikapidia.sr.pairwise;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.matrix.MatrixRow;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 * @author Ben Hillmann
 */


public class PairwiseCosineSimilarity implements PairwiseSimilarity {
    private static final Logger LOG = Logger.getLogger(PairwiseCosineSimilarity.class.getName());

    private TIntFloatHashMap lengths = null;   // lengths of each row
    private int maxResults = -1;
    private TIntSet idsInResults = new TIntHashSet();

    public PairwiseCosineSimilarity() {
    }

    public synchronized void initIfNeeded(SRMatrices matrices) {
        if (lengths == null) {
            LOG.info("building cached matrix information");
            lengths = new TIntFloatHashMap();
            for (SparseMatrixRow row : matrices.getFeatureMatrix()) {
                lengths.put(row.getRowIndex(), (float) row.getNorm());
                maxResults = Math.max(maxResults, row.getNumCols());
            }
            idsInResults.addAll(matrices.getFeatureTransposeMatrix().getRowIds());
        }
    }

    public double similarity(SRMatrices matrices, int wpId1, int wpId2) throws IOException {
        double sim = 0;
        MatrixRow row1 = matrices.getFeatureMatrix().getRow(wpId1);
        if (row1 != null) {
            MatrixRow row2 = matrices.getFeatureMatrix().getRow(wpId2);
            if (row2 != null) {
                    sim = cosineSimilarity(row1.asTroveMap(), row2.asTroveMap());
            }
        }
        return sim;
    }

    @Override
    public SRResultList mostSimilar(SRMatrices matrices, int wpId, int maxResults, TIntSet validIds) throws IOException {
        MatrixRow row = matrices.getFeatureMatrix().getRow(wpId);
        if (row == null) {
            LOG.info("unknown wpId: " + wpId);
            return new SRResultList(0);
        }
        initIfNeeded(matrices);

        TIntFloatHashMap vector = row.asTroveMap();
        TIntDoubleHashMap dots = new TIntDoubleHashMap();

        for (int id : vector.keys()) {
            float val1 = vector.get(id);
            MatrixRow row2 = matrices.getFeatureTransposeMatrix().getRow(id);
            if (row2 != null) {
                for (int j = 0; j < row2.getNumCols(); j++) {
                    int id2 = row2.getColIndex(j);
                    if (validIds == null || validIds.contains(id2)) {
                        float val2 = row2.getColValue(j);
                        dots.adjustOrPutValue(id2, val1 * val2, val1 * val2);
                    }
                }
            }
        }

        final Leaderboard leaderboard = new Leaderboard(maxResults);
        double rowNorm = norm(vector);
        for (int id : dots.keys()) {
            double l1 = lengths.get(id);
            double l2 = rowNorm;
            double dot = dots.get(id);
            double sim = dot / (l1 * l2);
            leaderboard.tallyScore(id, sim);
        }

        SRResultList result = leaderboard.getTop();
        result.sortDescending();
        return result;
    }


    private double cosineSimilarity(TIntFloatHashMap map1, TIntFloatHashMap map2) {
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;

        for (float x: map1.values()) { xDotX += x * x; }
        for (float y: map2.values()) { yDotY += y * y; }
        for (int id : map1.keys()) {
            if (map2.containsKey(id)) {
                xDotY += map1.get(id) * map2.get(id);
            }
        }
        return xDotY / Math.sqrt(xDotX * yDotY);
    }

    private double norm(TIntFloatHashMap vector) {
        double length = 0;
        for (float x : vector.values()) {
            length += x * x;
        }
        return Math.sqrt(length);
    }

    public double getMinValue() {
        return -1;
    }

    public double getMaxValue() {
        return +1;
    }

}
