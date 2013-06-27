package org.wikapidia.phrases;

import com.sleepycat.je.DatabaseException;
import com.typesafe.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.phrases.PhraseAnalyzerDao;
import org.wikapidia.phrases.PrunedCounts;
import org.wikapidia.utils.ObjectDb;
import org.wikapidia.utils.WpStringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Persists information about phrases to page relationships using an object database.
 */
public class PhraseAnalyzerObjectDbDao implements PhraseAnalyzerDao {
    private ObjectDb<PrunedCounts<String>> describeDb;
    private ObjectDb<PrunedCounts<Integer>> resolveDb;

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
            describeDb = new ObjectDb<PrunedCounts<String>>(new File(path, "describe"), isNew);
            resolveDb = new ObjectDb<PrunedCounts<Integer>>(new File(path, "resolve"), isNew);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }
    @Override
    public void savePageCounts(Language lang, int wpId, PrunedCounts<String> counts) throws DaoException {
        try {
            describeDb.put(lang.getLangCode() + ":" + wpId, counts);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void savePhraseCounts(Language lang, String phrase, PrunedCounts<Integer> counts) throws DaoException {
        phrase = normalize(phrase);
        try {
            resolveDb.put(lang.getLangCode() + ":" + phrase, counts);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }


    /**
     * Gets pages related to a phrase.
     *
     * @param lang
     * @param phrase
     * @param maxPages
     * @return Map from page ids (in the local language) to the number of occurrences
     * ordered by decreasing count.
     * @throws DaoException
     */
    @Override
    public PrunedCounts<Integer> getPhraseCounts(Language lang, String phrase, int maxPages) throws DaoException {
        phrase = normalize(phrase);
        try {
            PrunedCounts<Integer> counts = resolveDb.get(lang.getLangCode() + ":" + phrase);
            if (counts == null || counts.size() <= maxPages) {
                return counts;
            }
            PrunedCounts<Integer> result = new PrunedCounts<Integer>(counts.getTotal());
            for (int id : counts.keySet()) {
                if (result.size() >= maxPages) {
                    break;
                }
                result.put(id, counts.get(id));
            }
            return result;
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Normalizes a phrase by folding case and removing non alphanumeric characters.
     * This is scary for non-English languages!
     * @param phrase
     * @return
     */
    public String normalize(String phrase) {
        return WpStringUtils.normalize(phrase);
    }


    /**
     * Gets phrases related to a page.
     * @param lang
     * @param wpId Local page id
     * @param maxPhrases
     * @return Map from phrasese (in the local language) to the number of occurrences
     * ordered by decreasing count.
     * @throws DaoException
     */
    @Override
    public PrunedCounts<String> getPageCounts(Language lang, int wpId, int maxPhrases) throws DaoException {
        try {
            PrunedCounts<String> counts = describeDb.get(lang.getLangCode() + ":" + wpId);
            if (counts == null || counts.size() <= maxPhrases) {
                return counts;
            }
            PrunedCounts<String> result = new PrunedCounts<String>(counts.getTotal());
            for (String phrase : counts.keySet()) {
                if (result.size() >= maxPhrases) {
                    break;
                }
                result.put(phrase, counts.get(phrase));
            }
            return result;
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<PhraseAnalyzerDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PhraseAnalyzerDao.class;
        }

        @Override
        public String getPath() {
            return "phrases.dao";
        }

        @Override
        public PhraseAnalyzerDao get(String name, Config config) throws ConfigurationException {
            System.err.println("type is " + config.getString("type"));
            if (!config.getString("type").equals("objectdb")) {
                return null;
            }
            boolean isNew = config.getBoolean("isNew");
            File path = new File(config.getString("path"));
            try {
                return new PhraseAnalyzerObjectDbDao(path, isNew);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}