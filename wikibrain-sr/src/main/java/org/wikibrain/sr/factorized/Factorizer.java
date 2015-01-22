package org.wikibrain.sr.factorized;

import gnu.trove.map.TIntObjectMap;
import org.wikibrain.matrix.SparseMatrix;

/**
 * @author Shilad Sen
 */
public interface Factorizer {
    public TIntObjectMap<float[]> factorize(SparseMatrix adjacencies);
}
