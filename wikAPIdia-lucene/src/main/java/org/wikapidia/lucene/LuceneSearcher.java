package org.wikapidia.lucene;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
*
* @author Ari Weiland
*
*/
public class LuceneSearcher {

    private static final Logger LOG = Logger.getLogger(LuceneSearcher.class.getName());
    public static final int HIT_COUNT = 1000;

    protected LuceneOptions opts;

    private final File root;
    private final Map<Language, WikapidiaAnalyzer> analyzers;
    private final Map<Language, IndexSearcher> searchers;

    private int hitCount = HIT_COUNT;

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. Note that root is the parent directory
     * of the directory where lucene indexes are stored, though it is the same
     * directory as was passed to the LuceneIndexer.
     * @param languages
     * @param root the root directory in which each language contains its own lucene directory
     * @throws WikapidiaException
     */
    public LuceneSearcher(LanguageSet languages, File root) throws WikapidiaException {
        this(languages, root, new LuceneOptions());
    }

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. The directory is specified within opts.
     * @param languages
     * @param opts a LuceneOptions object containing specific options for lucene
     * @throws WikapidiaException
     */
    public LuceneSearcher(LanguageSet languages, LuceneOptions opts) throws WikapidiaException {
        this(languages, opts.luceneRoot, opts);
    }

    private LuceneSearcher(LanguageSet languages, File root, LuceneOptions opts) throws WikapidiaException {
        this.root = root;
        analyzers = new HashMap<Language, WikapidiaAnalyzer>();
        searchers = new HashMap<Language, IndexSearcher>();
        this.opts = opts;
        setup(languages);
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    /**
     * Runs a lucene query on the plaintext field for the specified
     * search string in the specified language.
     * @param searchString
     * @param language
     * @return
     * @throws WikapidiaException
     */
    public ScoreDoc[] search(String searchString, Language language) throws WikapidiaException {
        return search(opts.PLAINTEXT_FIELD_NAME, searchString, language);
    }

    /**
     * Runs a lucene query on the specified field for the specified
     * search string in the specified language.
     * @param fieldName
     * @param searchString
     * @param language
     * @return
     * @throws WikapidiaException
     */
    public ScoreDoc[] search(String fieldName, String searchString, Language language) throws WikapidiaException {
        try {
            QueryParser parser = new QueryParser(opts.matchVersion, fieldName, analyzers.get(language));
            Query query = parser.parse(searchString);
            return search(query, language);
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "Unable to parse " + searchString + " in " + language.getEnLangName());
            return null;
        }
    }

    /**
     * Runs a specified lucene query in the specified language
     * @param query
     * @param language
     * @return
     * @throws WikapidiaException
     */
    public ScoreDoc[] search(Query query, Language language) throws WikapidiaException {
        if (!analyzers.containsKey(language)) {
            throw new WikapidiaException("This Analyzer does not support " + language.getEnLangName());
        }
        try {
            return searchers.get(language).search(query, hitCount).scoreDocs;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }

    private void setup(LanguageSet languages) throws WikapidiaException {
        try {
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language, opts);
                analyzers.put(language, analyzer);
                Directory directory = FSDirectory.open(new File(root, language.getLangCode()));
                DirectoryReader reader = DirectoryReader.open(directory);
                searchers.put(language, new IndexSearcher(reader));
            }
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
