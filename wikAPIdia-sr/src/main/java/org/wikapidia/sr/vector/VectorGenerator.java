package org.wikapidia.sr.vector;

/**
 * @author Shilad Sen
 */

import gnu.trove.map.TIntFloatMap;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.matrix.SparseMatrix;

public interface VectorGenerator {

    /**
     * Returns the feature vector associated with Wikipedia id.
     * @param pageId
     * @return a sparse feature vector
     */
    public TIntFloatMap getVector(int pageId) throws DaoException;

    /**
     * Returns the feature vector associated with the phrase
     * @param phrase
     * @return a sparse feature vector
     * @throws UnsupportedOperationException if it cannot generate a feature vector for a phrase.
     */
    public TIntFloatMap getVector(String phrase);

    /**
     * Some vector implementations may already have a feature matrix available.
     * Otherwise, clients can create one manually.
     *
     * @return
     */
    public SparseMatrix getFeatureMatrix();

    /**
     * Some vector implementations may already have a feature transpose matrix available.
     * If there is not one, the Vector metric will need to create one.
     *
     * @return
     */
    public SparseMatrix getFeatureTransposeMatrix();
}
