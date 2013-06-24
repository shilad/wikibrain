package org.wikapidia.phrases.dao;

import com.sleepycat.je.DatabaseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.utils.ObjectDb;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Persists information about phrases to page relationships using an object database.
 */
public class PhraseAnalyzerObjectDbDao implements PhraseAnalyzerDao {
    private ObjectDb<DescriptionRecord> describeDb;
    private ObjectDb<ResolutionRecord> resolveDb;

    /**
     * Creates a new dao using the given directory.
     * @param path
     * @param isNew If true, delete any information contained in the directory.
     * @throws DaoException
     */
    public PhraseAnalyzerObjectDbDao(File path, boolean isNew) throws DaoException {
        if (isNew) {
            if (path.exists()) FileUtils.deleteQuietly(path);
            path.mkdirs();
        }
        try {
            describeDb = new ObjectDb<DescriptionRecord>(new File(path, "describe"), isNew);
            resolveDb = new ObjectDb<ResolutionRecord>(new File(path, "resolve"), isNew);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Adds information mapping a page to a phrase a certain number of times.
     * Multiple invocations of the method with the same page and phrase should sum the counts.
     * @param lang
     * @param wpId
     * @param phrase
     * @param count
     * @throws DaoException
     */
    @Override
    public void add(Language lang, int wpId, String phrase, int count) throws DaoException {
        addStoredPage(lang, wpId, phrase, count);
        addStoredPhrase(lang, phrase, wpId, count);
    }

    private void addStoredPage(Language lang, int wpId, String phrase, int count) throws DaoException {
        String key = lang.getLangCode() + wpId;
        DescriptionRecord value = null;
        try {
            value = describeDb.get(key);
            if (value == null) {
                value = new DescriptionRecord(wpId);
            }
            value.add(phrase, count);
            describeDb.put(key, value);
        } catch (DatabaseException e) {
            throw new DaoException(e);
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    private void addStoredPhrase(Language lang, String phrase, int wpId, int count) throws DaoException {
        String key = lang.getLangCode() + phrase;
        ResolutionRecord value = null;
        try {
            value = resolveDb.get(key);
            if (value == null) {
                value = new ResolutionRecord(phrase);
            }
            value.add(wpId, count);
            resolveDb.put(key, value);
        } catch (DatabaseException e) {
            throw new DaoException(e);
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Freeze records after all phrase to page relationships have been added.
     * Prune down records to meet a certain criteria
     * @param minCount
     * @param maxRank
     * @param minFrac
     * @throws DaoException
     */
    @Override
    public void freezeAndPrune(int minCount, int maxRank, double minFrac) throws DaoException {
        freezeAndPrune(resolveDb, minCount, maxRank, minFrac);
        freezeAndPrune(describeDb, minCount, maxRank, minFrac);
    }

    /**
     * Gets pages related to a phrase.
     *
     * @param lang
     * @param phrase
     * @return Map from page ids (in the local language) to the number of occurrences
     * ordered by decreasing count.
     * @throws DaoException
     */
    @Override
    public PrunedCounts<Integer> getPhraseCounts(Language lang, String phrase) throws DaoException {
        try {
            ResolutionRecord record = resolveDb.get(lang.getLangCode() + phrase);
            PrunedCounts<Integer> counts = new PrunedCounts<Integer>(record.getSum());
            for (int i = 0; i < record.size(); i++) {
                counts.put(record.getWpId(i), record.getCount(i));
            }
            return counts;
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }



    /**
     * Gets phrases related to a page.
     * @param lang
     * @param wpId Local page id
     * @return Map from phrasese (in the local language) to the number of occurrences
     * ordered by decreasing count.
     * @throws DaoException
     */
    @Override
    public PrunedCounts<String> getPageCounts(Language lang, int wpId) throws DaoException {
        try {
            DescriptionRecord record = describeDb.get(lang.getLangCode() + wpId);
            PrunedCounts<String> counts = new PrunedCounts<String>(record.getSum());
            for (int i = 0; i < record.size(); i++) {
                counts.put(record.getPhrase(i), record.getCount(i));
            }
            return counts;
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    /**
     *
     * @param db
     * @param minCount
     * @param maxRank
     * @param minFrac
     * @param <T>
     * @throws DaoException
     */

    private <T extends PrunableCounter> void freezeAndPrune(ObjectDb<T> db, int minCount, int maxRank, double minFrac) throws DaoException {
        Iterator<Pair<String, T>> iter = db.iterator();
        while (iter.hasNext()) {
            Pair<String, T> entry = iter.next();
            T record = entry.getValue();
            record.freeze();

            int counts[] = record.getCounts();
            int sum = 0;
            for (int i = 0; i < counts.length; i++) {
                sum += counts[i];
            }
            // find rank for this entry
            int i = 0;
            for (; i < counts.length  && i < maxRank; i++) {
                int c = counts[i];
                if (c < minCount || 1.0 * c / sum < minFrac) {
                    break;
                }
            }
            if (i == 0) {
                iter.remove();
            } else {
                if (i < counts.length) {
                    record.prune(i);
                }
                try {
                    db.put(entry.getKey(), record);
                } catch (DatabaseException e) {
                    throw new DaoException(e);
                } catch (IOException e) {
                    throw new DaoException(e);
                }
            }
        }
    }
}