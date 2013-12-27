package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntFloatMap;
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

    public synchronized void initIfNeeded(MostSimilarCache matrices) {
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

    @Override
    public SRResultList mostSimilar(MostSimilarCache matrices, TIntFloatMap vector, int maxResults, TIntSet validIds) throws IOException {
        initIfNeeded(matrices);
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

    @Override
    public SRResultList mostSimilar(MostSimilarCache matrices, int wpId, int maxResults, TIntSet validIds) throws IOException {
        MatrixRow row = matrices.getFeatureMatrix().getRow(wpId);
        if (row == null) {
            LOG.info("unknown wpId: " + wpId);
            return new SRResultList(0);
        }
        return mostSimilar(matrices, row.asTroveMap(), maxResults, validIds);
    }

    private double norm(TIntFloatMap vector) {
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
