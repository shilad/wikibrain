package org.wikapidia.sr.vector;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class MilneWittenSimilarity implements VectorSimilarity {
    private SparseMatrix feature;
    private SparseMatrix transpose;

    @Override
    public void setMatrices(SparseMatrix feature, SparseMatrix transpose) {
        this.feature = feature;
        this.transpose = transpose;
    }

    @Override
    public double similarity(TIntFloatMap vector1, TIntFloatMap vector2) {
        return 0;
    }

    @Override
    public SRResultList mostSimilar(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException {
        return null;
    }

    @Override
    public SRResult addExplanations(TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) {
        return null;
    }

    @Override
    public double getMinValue() {
        return 0;
    }

    @Override
    public double getMaxValue() {
        return 0;
    }
}
