package org.wikapidia.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class wraps the lucene search into a class that can handle any specified language
 *
 * @author Ari Weiland
 * @author Yulun Li
 *
*/
public class LuceneSearcher {

    private static final Logger LOG = Logger.getLogger(LuceneSearcher.class.getName());

    public static final int DEFAULT_HIT_COUNT = 1000;

    private final File root;
    private final Map<Language, IndexSearcher> searchers;
    private final Map<Language, DirectoryReader> readers;
    private final Map<Language, WikapidiaAnalyzer> analyzers;
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
            this.root = root;
            this.searchers = new HashMap<Language, IndexSearcher>();
            this.readers = new HashMap<Language, DirectoryReader>();
            this.analyzers = new HashMap<Language, WikapidiaAnalyzer>();
            for (Language language : languages) {
                Directory directory = FSDirectory.open(new File(root, language.getLangCode()));
                DirectoryReader reader = DirectoryReader.open(directory);
                readers.put(language, reader);
                searchers.put(language, new IndexSearcher(reader));
                analyzers.put(language, new WikapidiaAnalyzer(language, options));
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
    public WikapidiaScoreDoc[] search(Query query, Language language) {
        try {
            ScoreDoc[] scoreDocs = searchers.get(language).search(query, hitCount).scoreDocs;
            WikapidiaScoreDoc[] wikapidiaScoreDocs = new WikapidiaScoreDoc[scoreDocs.length];
            int i = 0;
            for (ScoreDoc scoreDoc : scoreDocs) {
                WikapidiaScoreDoc wikapidiaScoreDoc = new WikapidiaScoreDoc(scoreDoc.doc, scoreDoc.score);
                wikapidiaScoreDocs[i] = wikapidiaScoreDoc;
                i++;
            }
            return wikapidiaScoreDocs;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs a specified lucene query in the specified language with a specified hitcount.
     * @param query
     * @param language
     * @param hitCount
     * @return
     */
    public WikapidiaScoreDoc[] search(Query query, Language language, int hitCount) {
        try {
            this.hitCount = hitCount;
            ScoreDoc[] scoreDocs = searchers.get(language).search(query, hitCount).scoreDocs;
            WikapidiaScoreDoc[] wikapidiaScoreDocs = new WikapidiaScoreDoc[scoreDocs.length];
            int i = 0;
            for (ScoreDoc scoreDoc : scoreDocs) {
                WikapidiaScoreDoc wikapidiaScoreDoc = new WikapidiaScoreDoc(scoreDoc.doc, scoreDoc.score);
                wikapidiaScoreDocs[i] = wikapidiaScoreDoc;
                i++;
            }
            return wikapidiaScoreDocs;
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
                LOG.log(Level.WARNING, "This docId does not exist: " + docId);
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
        return readers.get(language);
    }

    public IndexSearcher getSearcherByLanguage(Language language) {
        return searchers.get(language);
    }

    public WikapidiaAnalyzer getAnalyzerByLanguage(Language language) {
        return analyzers.get(language);
    }

    public QueryBuilder getQueryBuilderByLanguage(Language language) {
        return new QueryBuilder(analyzers.get(language));
    }
}
