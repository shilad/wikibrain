package org.wikibrain.sr.vector;

/**
 * @author Shilad Sen
 */

import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.matrix.MatrixRow;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;

import java.io.File;
import java.io.IOException;

/**
 * Computes the similarity between two vectors, and calculates the most similar items for a vector.
 */
public interface VectorSimilarity {

    /**
     * Sets the matrix capturing features and transposes.
     * @param features
     * @param transpose
     * @param dataDir
     */
    public void setMatrices(SparseMatrix features, SparseMatrix transpose, File dataDir) throws IOException;

    /**
     * Returns the similarity of the two vectors.
     * @param vector1
     * @param vector2
     * @return
     */
    public double similarity(MatrixRow vector1, MatrixRow vector2);

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
     * @return The minimum possible score for the metric.
     */
    public double getMinValue();

    /**
     * @return The maximum possible score for the metric.
     */
    public double getMaxValue();
}
