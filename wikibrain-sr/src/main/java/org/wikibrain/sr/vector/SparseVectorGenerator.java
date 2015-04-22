package org.wikibrain.sr.vector;


import gnu.trove.map.TIntFloatMap;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;

import java.util.List;

/**
 * Generates sparse feature vectors for pages and phrases in some language.
 *
 * @author Shilad Sen
 */
public interface SparseVectorGenerator {

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
     * Adds the explanation for a particular SRResult if it is supported.
     * @param phrase1 First phrase
     * @param phrase2 Second phrase
     * @param vector1 Vector representing first item
     * @param vector2 Vector representing second item
     * @param result Original sr object, with explanations (hopefully) added.
     */
    public List<Explanation> getExplanations(String phrase1, String phrase2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException;

    /**
     * Adds the explanation for a particular SRResult if it is supported.
     * @param pageID1 First page
     * @param pageID2 Second page
     * @param vector1 Vector representing first item
     * @param vector2 Vector representing second item
     * @param result Original sr object, with explanations (hopefully) added.
     */
    public List<Explanation> getExplanations(int pageID1, int pageID2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException;
}
