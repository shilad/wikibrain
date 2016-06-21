package org.wikibrain.phrases;

import com.typesafe.config.Config;
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
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.utils.ObjectDb;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Persists information about phrases to page relationships using an object database.
 */
public class PhraseAnalyzerLangDao {
    private final StringNormalizer normalizer;
    private final Language lang;
    private File dir;

    private ObjectDb<PrunedCounts<String>> describeDb;
    private ObjectDb<PrunedCounts<Integer>> resolveDb;

    /**
     * Creates a new dao using the given directory.
     * @param path
     * @param isNew If true, delete any information contained in the directory.
     * @throws DaoException
     */
    public PhraseAnalyzerLangDao(StringNormalizer normalizer, Language lang, File path, boolean isNew) throws DaoException {
        this.dir = path;
        this.lang = lang;
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
    public void savePageCounts(int wpId, PrunedCounts<String> counts) throws DaoException {
        try {
            describeDb.put(""+wpId, counts);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    public void savePhraseCounts(String phrase, PrunedCounts<Integer> counts) throws DaoException {
        phrase = normalizer.normalize(lang, phrase);
        try {
            resolveDb.put(phrase, counts);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    public Iterator<String> getAllPhrases() {
        return resolveDb.keyIterator();
    }

    public Iterator<Pair<String, PrunedCounts<Integer>>> getAllPhraseCounts() {
        return resolveDb.iterator();
    }

    public PrunedCounts<Integer> getPhraseCounts(String phrase, int maxPages) throws DaoException {
        phrase = normalizer.normalize(lang, phrase);
        try {
            PrunedCounts<Integer> counts = resolveDb.get(phrase);
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

    public PrunedCounts<String> getPageCounts(int wpId, int maxPhrases) throws DaoException {
        try {
            PrunedCounts<String> counts = describeDb.get("" + wpId);
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

    public void flush() {
        this.describeDb.flush();
        this.resolveDb.flush();
    }

    public void close() {
        this.describeDb.close();
        this.resolveDb.close();
    }
}
