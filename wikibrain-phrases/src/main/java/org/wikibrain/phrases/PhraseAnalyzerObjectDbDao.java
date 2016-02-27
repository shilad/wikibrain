package org.wikibrain.phrases;

import com.typesafe.config.Config;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persists information about phrases to page relationships using an object database.
 */
public class PhraseAnalyzerObjectDbDao implements PhraseAnalyzerDao {
    private static final Logger LOG = LoggerFactory.getLogger(PhraseAnalyzerObjectDbDao.class);

    private final Map<Language, PhraseAnalyzerLangDao> langDaos = new HashMap<Language, PhraseAnalyzerLangDao>();
    private final File dir;
    private final boolean isNew;
    private final StringNormalizer normalizer;

    /**
     * Creates a new dao using the given directory.
     * @param path
     * @param isNew If true, delete any information contained in the directory.
     * @throws DaoException
     */
    public PhraseAnalyzerObjectDbDao(StringNormalizer normalizer, File path, boolean isNew) throws DaoException {
        this.dir = path;
        this.isNew = isNew;
        this.normalizer = normalizer;

        if (isNew) {
            if (path.exists()) FileUtils.deleteQuietly(path);
            path.mkdirs();
        }
    }

    synchronized PhraseAnalyzerLangDao getDao(Language lang) throws DaoException {
        File subDir = new File(dir, lang.getLangCode());
        if (langDaos.containsKey(lang)) {
            return langDaos.get(lang);
        } else if (subDir.isDirectory() || isNew) {
            langDaos.put(lang, new PhraseAnalyzerLangDao(normalizer, lang, subDir, isNew));
            return langDaos.get(lang);
        } else {
//            throw new DaoException("No phrase dao available for " + lang);
            return null;
        }
    }

    @Override
    public void savePageCounts(Language lang, int wpId, PrunedCounts<String> counts) throws DaoException {
        getDao(lang).savePageCounts(wpId, counts);
    }

    @Override
    public void savePhraseCounts(Language lang, String phrase, PrunedCounts<Integer> counts) throws DaoException {
        getDao(lang).savePhraseCounts(phrase, counts);
    }

    @Override
    public Iterator<String> getAllPhrases(final Language lang) {
        try {
            PhraseAnalyzerLangDao dao = getDao(lang);
            return (dao == null)
                    ? new ArrayList<String>().iterator()
                    : dao.getAllPhrases();
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<Pair<String, PrunedCounts<Integer>>> getAllPhraseCounts(final Language lang) {
        try {
            PhraseAnalyzerLangDao dao = getDao(lang);
            return (dao == null)
                    ? new ArrayList<Pair<String, PrunedCounts<Integer>>>().iterator()
                    : dao.getAllPhraseCounts();
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StringNormalizer getStringNormalizer() {
        return normalizer;
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
        PhraseAnalyzerLangDao dao = getDao(lang);
        return (dao == null) ? null : dao.getPhraseCounts(phrase, maxPages);
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
        PhraseAnalyzerLangDao dao = getDao(lang);
        return (dao == null) ? null : dao.getPageCounts(wpId, maxPhrases);
    }

    @Override
    public void flush() {
        for (PhraseAnalyzerLangDao dao : langDaos.values()) {
            dao.flush();
        }
    }

    @Override
    public void close() {
        for (PhraseAnalyzerLangDao dao : langDaos.values()) {
            dao.close();
        }
    }

    public static class Provider extends org.wikibrain.conf.Provider<PhraseAnalyzerDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<PhraseAnalyzerDao> getType() {
            return PhraseAnalyzerDao.class;
        }

        @Override
        public String getPath() {
            return "phrases.dao";
        }

        @Override
        public PhraseAnalyzerDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("objectdb")) {
                return null;
            }
            boolean isNew = config.getBoolean("isNew");

            File path = new File(getConfig().get().getString("phrases.path"), name);
            StringNormalizer normalizer = getConfigurator().get(StringNormalizer.class, config.getString("normalizer"));

            try {
                return new PhraseAnalyzerObjectDbDao(normalizer, path, isNew);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
