package org.wikapidia.sr.vector;

/**
 * @author Shilad Sen
 */

import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;

import java.io.IOException;

/**
 * Computes the similarity between two vectors, and calculates the most similar items for a vector.
 */
public interface VectorSimilarity {

    /**
     * Sets the matrix capturing features and transposes.
     * @param features
     * @param transpose
     */
    public void setMatrices(SparseMatrix features, SparseMatrix transpose);

    /**
     * Computes the similarity between the two vectors.
     * @param vector1
     * @param vector2
     * @return
     */
    public double similarity(TIntFloatMap vector1, TIntFloatMap vector2);

    /**
     * Returns the most similar items for a particular vector.
     * @param query
     * @param maxResults
     * @param validIds
     * @return
     */
    public SRResultList mostSimilar(TIntFloatMap query, int maxResults, TIntSet validIds) throws IOException;

    /**
     * Adds the explanation for a particular SRResult if it is supported.
     * @param vector1
     * @param vector2
     * @param result
     * @return
     */
    public SRResult addExplanations(TIntFloatMap vector1, TIntFloatMap vector2, SRResult result);

    /**
     * @return The minimum possible score for the metric.
     */
    public double getMinValue();

    /**
     * @return The maximum possible score for the metric.
     */
    public double getMaxValue();
}
