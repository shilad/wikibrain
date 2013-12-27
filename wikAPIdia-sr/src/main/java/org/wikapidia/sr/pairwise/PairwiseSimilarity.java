package org.wikapidia.sr.pairwise;

import gnu.trove.map.TIntFloatMap;
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
    public SRResultList mostSimilar(MostSimilarCache matrices, TIntFloatMap vector, int maxResults, TIntSet validIds) throws IOException;
    public SRResultList mostSimilar(MostSimilarCache matrices, int wpId, int maxResults, TIntSet validIds) throws IOException;
}
