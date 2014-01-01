package org.wikapidia.sr.vector;


import gnu.trove.map.TIntFloatMap;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.SRResult;

import java.util.List;

/**
 * Generates sparse feature vectors for pages and phrases in some language.
 *
 * @author Shilad Sen
 */
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
     * Adds the explanation for a particular SRResult if it is supported.
     * @param page1 First page
     * @param page2 Second page
     * @param vector1 Vector representing first item
     * @param vector2 Vector representing second item
     * @param result Original sr object, with explanations (hopefully) added.
     */
    public List<Explanation> getExplanations(LocalPage page1, LocalPage page2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException;
}
