package org.wikapidia.sr.pairwise;

import gnu.trove.set.TIntSet;
import org.wikapidia.sr.SRResultList;

import java.io.IOException;

/**
 * Minimal interface needed for vector-based cosimilarity matrices.
 *
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public interface PairwiseSimilarity {
    public double getMinValue();
    public double getMaxValue();
    public SRResultList mostSimilar(SRMatrices matrices, int wpId, int maxResults, TIntSet validIds) throws IOException;
}
