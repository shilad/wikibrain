package org.wikibrain.phrases;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Given a page, returns the most common phrases
 */
public interface PhraseAnalyzer {

    /**
     * Loads a specific corpus into the dao.
     * Returns the number of phrases loaded.
     *
     * @throws DaoException
     * @throws IOException
     */
    int loadCorpus(LanguageSet langs) throws DaoException, IOException;

    /**
     * Returns the most descriptive phrases for a wikipedia page.
     * @param language The language for the phrase and the returned LocalPages.
     * @param page The page to be described.
     * @param maxPhrases The maximum number of phrases to be returned.
     * @return An map from phrase to score, ordered by decreasing probability.
     * The scores can be considered probabilities that sum to 1.0 across all possibilities.
     */
    public LinkedHashMap<String, Float> describe(Language language, LocalPage page, int maxPhrases) throws DaoException;

    /**
     * Returns the most likely wikipedia pages for a phrase.
     * @param language The language for the phrase and the returned LocalPages.
     * @param phrase The phrase to be resolved.
     * @param maxPages The maximum number of pages to be returned.
     * @return An map from page to score, ordered by decreasing probability.
     * The scores can be considered probabilities that sum to 1.0 across all possibilities.
     */
    public LinkedHashMap<LocalId, Float> resolve(Language language, String phrase, int maxPages) throws DaoException;

}
