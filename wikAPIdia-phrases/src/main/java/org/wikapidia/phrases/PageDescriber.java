package org.wikapidia.phrases;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.util.LinkedHashMap;

/**
 * Given a page, returns the most common phrases
 */
public interface PageDescriber {
    /**
     * Returns the most descriptive phrases for a wikipedia page.
     * @param language The language for the phrase and the returned LocalPages.
     * @param page The page to be described.
     * @param maxPhrases The maximum number of phrases to be returned.
     * @return An map from phrase to score, ordered by decreasing probability.
     * The scores can be considered probabilities that sum to 1.0 across all possibilities.
     */
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) throws DaoException;

    /**
     * Returns the most descriptive phrases for a universal page.
     * @param language The language for the returned phrases.
     * @param page The page to be described.
     * @param maxPhrases The maximum number of phrases to be returned.
     * @return An map from phrase to score, ordered by decreasing probability.
     * The scores can be considered probabilities that sum to 1.0 across all possibilities.
     */
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases);
}
