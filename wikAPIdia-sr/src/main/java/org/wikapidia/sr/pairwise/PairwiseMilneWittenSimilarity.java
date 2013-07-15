package org.wikapidia.sr.pairwise;

import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.matrix.MatrixRow;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.MilneWittenCore;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class PairwiseMilneWittenSimilarity implements PairwiseSimilarity {

    private static final Logger LOG = Logger.getLogger(PairwiseCosineSimilarity.class.getName());

    private SparseMatrix matrix;
    private SparseMatrix transpose;
    private TIntFloatHashMap lengths = null;   // lengths of each row
    private int maxResults = -1;
    private TIntSet idsInResults = new TIntHashSet();
    MilneWittenCore milneWittenCore = new MilneWittenCore();

    public PairwiseMilneWittenSimilarity(String path) throws IOException {
        this.matrix = new SparseMatrix(new File(path+"-feature"));
        this.transpose = new SparseMatrix(new File(path+"-transpose"));
    }

    public double similarity(int wpID1, int wpId2) throws IOException {
        double sim = 0;
        MatrixRow row1 = matrix.getRow(wpID1);
        if (row1 != null) {
            MatrixRow row2 = matrix.getRow(wpId2);
            if (row2 != null) {
                sim = milneWittenSimilarity(row1.asTroveMap(), row2.asTroveMap());
            }
        }
        return sim;
    }

    @Override
    public SRResultList mostSimilar(int wpId, int maxResults, TIntSet validIds) throws IOException {
        Leaderboard leaderboard = new Leaderboard(maxResults);
        for (int id: validIds.toArray()) {
            leaderboard.tallyScore(id, similarity(wpId, id));
        }
        return leaderboard.getTop();
    }

    private double milneWittenSimilarity(TIntFloatHashMap map1, TIntFloatHashMap map2) {
        TIntSet A = new TIntHashSet();
        TIntSet B = new TIntHashSet();

        for(int key: map1.keys()) {
            if (map1.get(key) == 1) {
                A.add(key);
            }
            if (map2.get(key) == 1) {
                B.add(key);
            }
        }

        return milneWittenCore.similarity(A, B, map1.size(), false).getValue();
    }


}
