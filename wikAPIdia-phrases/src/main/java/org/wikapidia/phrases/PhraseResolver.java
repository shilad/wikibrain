package org.wikapidia.phrases;

import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.util.LinkedHashMap;

/**
 * For a given phrase, returns the most common pages.
 *
 * If you want smarter, contex aware disambiguation, look at disambig in the sr module.
 */
public interface PhraseResolver {
    /**
     * Returns the most likely wikipedia pages for a phrase.
     * @param language The language for the phrase and the returned LocalPages.
     * @param phrase The phrase to be resolved.
     * @param maxPages The maximum number of pages to be returned.
     * @return An map from page to score, ordered by decreasing probability.
     * The scores can be considered probabilities that sum to 1.0 across all possibilities.
     */
    public LinkedHashMap<LocalPage, Float> resolveLocal(Language language, String phrase, int maxPages);

    /**
     * Returns the most likely universal pages for a phrase.
     * @param language The language for the phrase.
     * @param phrase The phrase to be resolved.
     * @param maxPages The maximum number of pages to be returned.
     * @return An map from page to score, ordered by decreasing probability.
     * The scores can be considered probabilities that sum to 1.0 across all possibilities.
     */
    public LinkedHashMap<UniversalPage, Float> resolveUniversal(Language language, String phrase, int algorithmId, int maxPages);
}
