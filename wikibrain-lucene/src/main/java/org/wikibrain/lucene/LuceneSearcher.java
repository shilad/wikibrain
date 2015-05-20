package org.wikibrain.lucene;

import com.typesafe.config.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This class wraps the lucene search into a class that can handle any specified language
 *
 * @author Ari Weiland
 * @author Yulun Li
 *
*/
public class LuceneSearcher {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneSearcher.class);

    public static final int DEFAULT_HIT_COUNT = 1000;

    private final File root;
    private final Map<Language, IndexSearcher> searchers;
    private final Map<Language, DirectoryReader> readers;
    private final Map<Language, WikiBrainAnalyzer> analyzers;
    private final LuceneOptions options;

    private int hitCount = DEFAULT_HIT_COUNT;

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. Note that root is the parent directory
     * of the directory where lucene indexes are stored, though it is the same
     * directory as was passed to the LuceneIndexer.
     *
     * @param languages the language set in which this searcher can operate
     * @param root the root directory in which each language contains its own lucene directory
     */
    public LuceneSearcher(LanguageSet languages, File root) {
        this(languages, root, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. The directory is specified within options.
     *
     * @param languages the language set in which this searcher can operate
     * @param options a LuceneOptions object containing specific options for lucene
     */
    public LuceneSearcher(LanguageSet languages, LuceneOptions options) {
        this(languages, options.luceneRoot, options);
    }

    private LuceneSearcher(LanguageSet languages, File root, LuceneOptions options) {
        try {
            System.err.println("LOADING LANGUAGES " + languages);
            this.root = root;
            this.searchers = new HashMap<Language, IndexSearcher>();
            this.readers = new HashMap<Language, DirectoryReader>();
            this.analyzers = new HashMap<Language, WikiBrainAnalyzer>();
            for (Language language : languages) {
                File langRoot = new File(root, language.getLangCode());
                if (!langRoot.isDirectory()) {
                    throw new IllegalArgumentException("no index at location: " + langRoot);
                }
                Directory directory = FSDirectory.open(langRoot);
                DirectoryReader reader = DirectoryReader.open(directory);
                readers.put(language, reader);
                searchers.put(language, new IndexSearcher(reader));
                analyzers.put(language, new WikiBrainAnalyzer(language, options));
            }
            this.options = options;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getRoot() {
        return root;
    }

    public LanguageSet getLanguageSet() {
        return new LanguageSet(searchers.keySet());
    }

    public LuceneOptions getOptions() {
        return options;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    /**
     * Runs a specified lucene query in the specified language.
     *
     * @param query
     * @return
     */
    public WikiBrainScoreDoc[] search(Query query, Language language) {
        return search(query, language, this.hitCount, null);
    }

    public WikiBrainScoreDoc[] search(Query query, Language language, int hitCount) {
        return search(query, language, hitCount, null);
    }

    /**
     * Runs a specified lucene query in the specified language with a specified hitcount.
     * @param query
     * @param language
     * @param hitCount
     * @return
     */
    public WikiBrainScoreDoc[] search(Query query, Language language, int hitCount, Filter filter) {
        return search(query, language, hitCount, filter, true);

    }

    /**
     * Runs a specified lucene query in the specified language with a specified hitcount.
     * @param query
     * @param language
     * @param hitCount
     * @param filter
     * @param resolveWpIds if True, returns wikipedia ids. otherwise returns lucene ids.
     * @return
     */
    public WikiBrainScoreDoc[] search(Query query, Language language, int hitCount, Filter filter, boolean resolveWpIds) {
        if (!searchers.containsKey(language)) throw new IllegalArgumentException("Unknown language: " + language);
        try {
            this.hitCount = hitCount;
            ScoreDoc[] scoreDocs = searchers.get(language).search(query, filter, hitCount).scoreDocs;
            WikiBrainScoreDoc[] wikibrainScoreDocs = new WikiBrainScoreDoc[scoreDocs.length];
            for (int i = 0; i < scoreDocs.length; i++) {
                ScoreDoc scoreDoc = scoreDocs[i];
                int wpId = resolveWpIds ? getLocalIdFromDocId(scoreDoc.doc, language) : -1;
                wikibrainScoreDocs[i] = new WikiBrainScoreDoc(scoreDoc.doc, wpId, scoreDoc.score);
            }
            return wikibrainScoreDocs;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the local ID for a specified lucene document,
     * within a given language.
     *
     * @param docId
     * @param language
     * @return
     */
    public int getLocalIdFromDocId(int docId, Language language) {
        try {
            if (docId != -1) {
                Document document = searchers.get(language).doc(docId);
                return (Integer) document.getField(LuceneOptions.LOCAL_ID_FIELD_NAME).numericValue();
            } else {
                LOG.warn("This docId does not exist: " + docId);
                return -1;
            }
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getDocIdFromLocalId(int localId, Language language) throws DaoException {
        Query query = NumericRangeQuery.newIntRange(LuceneOptions.LOCAL_ID_FIELD_NAME, localId, localId, true, true);
        try {
            ScoreDoc[] hits = searchers.get(language).search(query, 1).scoreDocs;
            if (hits.length == 0) {
                return -1;
            } else {
                return hits[0].doc;
            }
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    public DirectoryReader getReaderByLanguage(Language language) {
        if (!readers.containsKey(language)) throw new IllegalArgumentException("Unknown language: " + language);
        return readers.get(language);
    }

    public IndexSearcher getSearcherByLanguage(Language language) {
        if (!searchers.containsKey(language)) throw new IllegalArgumentException("Unknown language: " + language);
        return searchers.get(language);
    }

    public WikiBrainAnalyzer getAnalyzerByLanguage(Language language) {
        if (!analyzers.containsKey(language)) throw new IllegalArgumentException("Unknown language: " + language);
        return analyzers.get(language);
    }

    public QueryBuilder getQueryBuilderByLanguage(Language language) {
        if (!analyzers.containsKey(language)) throw new IllegalArgumentException("Unknown language: " + language);
        return new QueryBuilder(this, language);
    }

    public static class Provider extends org.wikibrain.conf.Provider<LuceneSearcher> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LuceneSearcher.class;
        }

        @Override
        public String getPath() {
            return "lucene.searcher";
        }

        @Override
        public LuceneSearcher get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            return new LuceneSearcher(
                    getConfigurator().get(LanguageSet.class),
                    getConfigurator().get(LuceneOptions.class, config.getString("options"))
            );
        }
    }
}
