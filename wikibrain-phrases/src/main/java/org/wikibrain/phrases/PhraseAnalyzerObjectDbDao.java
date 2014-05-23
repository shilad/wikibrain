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
import java.util.Iterator;
import java.util.Map;

/**
 * Persists information about phrases to page relationships using an object database.
 */
public class PhraseAnalyzerObjectDbDao implements PhraseAnalyzerDao {
    private final StringNormalizer normalizer;
    private File dir;
    private ObjectDb<PrunedCounts<String>> describeDb;
    private ObjectDb<PrunedCounts<Integer>> resolveDb;

    /**
     * Creates a new dao using the given directory.
     * @param path
     * @param isNew If true, delete any information contained in the directory.
     * @throws DaoException
     */
    public PhraseAnalyzerObjectDbDao(StringNormalizer normalizer, File path, boolean isNew) throws DaoException {
        this.dir = path;
        this.normalizer = normalizer;
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
        phrase = normalizer.normalize(lang, phrase);
        try {
            resolveDb.put(lang.getLangCode() + ":" + phrase, counts);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public Iterator<String> getAllPhrases(final Language lang) {
        Predicate langFilter = new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    String langCode = ((String)o).split(":")[0];
                    return lang.getLangCode().equals(langCode);
                }
            };
        Transformer stripLang = new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((String)input).split(":", 2)[1];
            }
        };
        return new TransformIterator(
                    new FilterIterator(resolveDb.keyIterator(), langFilter),
                    stripLang);
    }

    @Override
    public Iterator<Pair<String, PrunedCounts<Integer>>> getAllPhraseCounts(final Language lang) {
        Predicate langFilter = new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                Pair<String, PrunedCounts<Integer>> pair = (Pair<String, PrunedCounts<Integer>>) o;
                String langCode = pair.getLeft().split(":")[0];
                return lang.getLangCode().equals(langCode);
            }
        };
        Transformer stripLang = new Transformer() {
            @Override
            public Object transform(Object o) {
                Pair<String, PrunedCounts<Integer>> pair = (Pair<String, PrunedCounts<Integer>>) o;
                String phrase = pair.getLeft().split(":", 2)[1];
                return Pair.of(phrase, pair.getRight());
            }
        };
        return new TransformIterator(
                new FilterIterator(resolveDb.iterator(), langFilter),
                stripLang);
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
        phrase = normalizer.normalize(lang, phrase);
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

    @Override
    public void flush() {
        this.describeDb.flush();
        this.resolveDb.flush();
    }

    @Override
    public void close() {
        this.describeDb.close();
        this.resolveDb.close();
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
