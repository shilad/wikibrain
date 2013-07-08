package org.wikapidia.lucene;

import org.apache.lucene.index.DirectoryReader;
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
import java.util.logging.Logger;

/**
*
* @author Ari Weiland
*
*/
public class LuceneSearcher {

    private static final Logger LOG = Logger.getLogger(LuceneSearcher.class.getName());
    public static final int HIT_COUNT = 1000;

    private final LuceneOptions options;
    private final File root;
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
        this(languages, root, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a LuceneSearcher that will run lucene queries on sets of articles
     * in any language in the LanguageSet. The directory is specified within options.
     * @param languages
     * @param options a LuceneOptions object containing specific options for lucene
     * @throws WikapidiaException
     */
    public LuceneSearcher(LanguageSet languages, LuceneOptions options) throws WikapidiaException {
        this(languages, options.luceneRoot, options);
    }

    private LuceneSearcher(LanguageSet languages, File root, LuceneOptions options) throws WikapidiaException {
        try {
            this.root = root;
            this.searchers = new HashMap<Language, IndexSearcher>();
            for (Language language : languages) {
                Directory directory = FSDirectory.open(new File(root, language.getLangCode()));
                DirectoryReader reader = DirectoryReader.open(directory);
                searchers.put(language, new IndexSearcher(reader));
            }
            this.options = options;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
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
     * Runs a specified lucene query in the specified language
     * @param query
     * @return
     * @throws WikapidiaException
     */
    public ScoreDoc[] search(Query query, Language language) throws WikapidiaException {
        try {
            return searchers.get(language).search(query, hitCount).scoreDocs;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
