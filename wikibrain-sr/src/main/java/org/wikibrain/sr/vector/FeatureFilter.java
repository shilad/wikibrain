package org.wikibrain.sr.vector;

import gnu.trove.map.TIntFloatMap;
import org.wikibrain.matrix.SparseMatrixRow;

/**
 * Can be used to filter a feature vector. Useful for training / testing purposes.
 *
 * @author Shilad Sen
 */
public interface FeatureFilter {
    public TIntFloatMap filter(int page, TIntFloatMap vector);
    public SparseMatrixRow filter(int page, SparseMatrixRow vector);
}
