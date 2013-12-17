package org.wikapidia.sr.pairwise;

import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.matrix.MatrixRow;
import org.wikapidia.sr.milnewitten.MilneWittenCore;
import org.wikapidia.sr.SRResultList;
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

    MilneWittenCore milneWittenCore = new MilneWittenCore();

    public PairwiseMilneWittenSimilarity() {
    }

    public double similarity(SRMatrices matrices, int wpID1, int wpId2) throws IOException {
        double sim = 0;
        MatrixRow row1 = matrices.getFeatureMatrix().getRow(wpID1);
        if (row1 != null) {
            MatrixRow row2 = matrices.getFeatureMatrix().getRow(wpId2);
            if (row2 != null) {
                sim = milneWittenSimilarity(row1.asTroveMap(), row2.asTroveMap());
            }
        }
        return sim;
    }

    @Override
    public SRResultList mostSimilar(SRMatrices matrices, int wpId, int maxResults, TIntSet validIds) throws IOException {
        Leaderboard leaderboard = new Leaderboard(maxResults);
        TIntFloatHashMap rowA = matrices.getFeatureMatrix().getRow(wpId).asTroveMap();
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

        if (validIds==null){
            validIds=possibleIds;
        } else {
            validIds.retainAll(possibleIds);
        }

        for (int id: validIds.toArray()) {
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

    private double milneWittenSimilarity(TIntFloatHashMap map1, TIntFloatHashMap map2) {
        TIntSet a = new TIntHashSet();
        TIntSet b = new TIntHashSet();

        for(int key: map1.keys()) {
            if (map1.get(key) == 1) {
                a.add(key);
            }
            if (map2.get(key) == 1) {
                b.add(key);
            }
        }

        return milneWittenCore.similarity(a, b, map1.size(), false).getScore();
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
