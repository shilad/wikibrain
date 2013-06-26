package org.wikapidia.phrases.dao;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.util.LinkedHashMap;

/**
 * Stores and retrieves information related to phrase to page relationships.
 * @author Shilad Sen
 */
public interface PhraseAnalyzerDao {

    /**
     * Captures the top-k entries, by count, but also remembers the total count.
     * @param <K> The class of the objects being counted.
     */
    public static class PrunedCounts<K> extends LinkedHashMap<K, Integer> {
        private int total;
        public PrunedCounts(int total) { this.total = total; }
        public int getTotal() { return total; }
    }

    /**
     * Adds information mapping a page to a phrase a certain number of times.
     * Multiple invocations of the method with the same page and phrase should sum the counts.
     * @param lang
     * @param wpId
     * @param phrase
     * @param count
     * @throws org.wikapidia.core.dao.DaoException
     */
    void add(Language lang, int wpId, String phrase, int count) throws DaoException;

    /**
     * Freeze records after all phrase to page relationships have been added.
     * Prune down records to meet a certain criteria
     * @param minCount
     * @param maxRank
     * @param minFrac
     * @throws org.wikapidia.core.dao.DaoException
     */
    void freezeAndPrune(int minCount, int maxRank, double minFrac) throws DaoException;

    /**
     * Gets pages related to a phrase.
     * @param lang
     * @param phrase
     * @return Map from page ids (in the local language) to the number of occurrences
     * ordered by decreasing count.
     * @throws DaoException
     */
    PrunedCounts<Integer> getPhraseCounts(Language lang, String phrase) throws DaoException;

    /**
     * Gets phrases related to a page.
     * @param lang
     * @param wpId Local page id
     * @return Map from phrasese (in the local language) to the number of occurrences
     * ordered by decreasing count.
     * @throws DaoException
     */
    PrunedCounts<String> getPageCounts(Language lang, int wpId) throws DaoException;
}
