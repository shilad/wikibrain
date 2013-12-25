package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.matrix.MatrixRow;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.milnewitten.MilneWittenCore;
import org.wikapidia.sr.utils.Leaderboard;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class PairwiseMilneWittenSimilarity implements PairwiseSimilarity {
    private static final double epsilon = 0.00001;

    private static final Logger LOG = Logger.getLogger(PairwiseCosineSimilarity.class.getName());

    public PairwiseMilneWittenSimilarity() {
    }

    @Override
    public SRResultList mostSimilar(SRMatrices matrices, TIntFloatMap rowA, int maxResults, TIntSet validIds) throws IOException {
        Leaderboard leaderboard = new Leaderboard(maxResults);
        int sizeA = 0;
        TIntSet linkIds = new TIntHashSet();
        for (int i : rowA.keys()){
            if (Math.abs(rowA.get(i)-1)<epsilon){
                sizeA++;
                linkIds.add(i);
            }
        }

        TIntSet possibleIds = new TIntHashSet();
        for (int i: linkIds.toArray()){
            TIntFloatHashMap finderRow = matrices.getFeatureTransposeMatrix().getRow(i).asTroveMap();
            for (int j : finderRow.keys()){
                if (Math.abs(finderRow.get(j)-1)<epsilon){
                    possibleIds.add(j);
                }
            }
        }

        if (validIds != null) {
            possibleIds.retainAll(validIds);
        }

        for (int id: possibleIds.toArray()) {
            TIntFloatHashMap rowB = matrices.getFeatureMatrix().getRow(id).asTroveMap();
            if (rowB != null){
                int sizeB = 0;
                int intersection = 0;
                for (int key : rowB.keys()){
                    if (Math.abs(rowB.get(key)-1)<epsilon){
                        sizeB++;
                        if (Math.abs(rowA.get(key)-1)<epsilon){
                            intersection++;
                        }
                    }
                }
                double similarity = 1- (Math.log(Math.max(sizeA,sizeB))-Math.log(intersection))
                        / (Math.log(matrices.getFeatureMatrix().getNumRows())-Math.log(Math.min(sizeA,sizeB)));
                leaderboard.tallyScore(id, similarity);
            }
        }
        SRResultList result = leaderboard.getTop();
        result.sortDescending();
        return result;
    }

    @Override
    public SRResultList mostSimilar(SRMatrices matrices, int wpId, int maxResults, TIntSet validIds) throws IOException {
        SparseMatrixRow row = matrices.getFeatureMatrix().getRow(wpId);
        if (row == null) {
            return new SRResultList(0);
        }
        return mostSimilar(matrices, row.asTroveMap(), maxResults, validIds);
    }

    @Override
    public double getMaxValue() {
        return 1.0;
    }

    @Override
    public double getMinValue() {
        return 0.0;
    }


}
