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

    private final LuceneOptions opts;
    private final File root;
    private final WikapidiaAnalyzer analyzer;
    private final IndexSearcher searcher;

    private int hitCount = HIT_COUNT;

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. Note that root is the parent directory
     * of the directory where lucene indexes are stored, though it is the same
     * directory as was passed to the LuceneIndexer.
     * @param language
     * @param root the root directory in which each language contains its own lucene directory
     * @throws WikapidiaException
     */
    public LuceneSearcher(Language language, File root) throws WikapidiaException {
        this(language, root, new LuceneOptions());
    }

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. The directory is specified within opts.
     * @param language
     * @param opts a LuceneOptions object containing specific options for lucene
     * @throws WikapidiaException
     */
    public LuceneSearcher(Language language, LuceneOptions opts) throws WikapidiaException {
        this(language, opts.luceneRoot, opts);
    }

    private LuceneSearcher(Language language, File root, LuceneOptions opts) throws WikapidiaException {
        try {
            this.root = root;
            this.analyzer = new WikapidiaAnalyzer(language, opts); // TODO: TokenizerOptions are always set to default. Should we add more user control?
            Directory directory = FSDirectory.open(new File(root, language.getLangCode()));
            DirectoryReader reader = DirectoryReader.open(directory);
            this.searcher = new IndexSearcher(reader);
            this.opts = opts;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }

    public LuceneOptions getOpts() {
        return opts;
    }

    public WikapidiaAnalyzer getAnalyzer() {
        return analyzer;
    }

    public Language getLanguage() {
        return analyzer.getLanguage();
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
        return search(LuceneOptions.PLAINTEXT_FIELD_NAME, searchString);
    }

    /**
     * Runs a lucene query on the specified field for the specified search string.
     * @param fieldName
     * @param searchString
     * @return
     * @throws WikapidiaException
     */
    public ScoreDoc[] search(String fieldName, String searchString) throws WikapidiaException {
        try {
            QueryParser parser = new QueryParser(opts.matchVersion, fieldName, analyzer);
            Query query = parser.parse(searchString);
            return search(query);
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "Unable to parse " + searchString);
            return null;
        }
    }

    /**
     * Runs a specified lucene query
     * @param query
     * @return
     * @throws WikapidiaException
     */
    public ScoreDoc[] search(Query query) throws WikapidiaException {
        try {
            return searcher.search(query, hitCount).scoreDocs;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
