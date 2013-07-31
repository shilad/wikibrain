package org.wikapidia.sr.pairwise;

import gnu.trove.set.TIntSet;
import org.wikapidia.sr.SRResultList;

import java.io.IOException;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public interface PairwiseSimilarity {

    public void initMatrices (String path) throws IOException;

    public SRResultList mostSimilar(int wpId, int maxResults, TIntSet validIds) throws IOException;

}
