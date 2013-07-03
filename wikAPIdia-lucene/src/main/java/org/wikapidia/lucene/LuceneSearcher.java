package org.wikapidia.lucene;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.wikapidia.conf.Configuration;
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
    public static final int HIT_COUNT = 1000; // TODO: do something more user-friendly with this value?

    protected final LuceneOptions O = new LuceneOptions(new Configuration());

    private final File file;
    private final Map<Language, WikapidiaAnalyzer> analyzers;
    private final Map<Language, IndexSearcher> searchers;

    /**
     * Constructs a LuceneSearcher that will run lucene queries on
     * sets of articles in any language in the LanguageSet.
     * @param languages
     * @throws WikapidiaException
     */
    public LuceneSearcher(LanguageSet languages) throws WikapidiaException {
        try {
            file = O.LUCENE_ROOT;
            analyzers = new HashMap<Language, WikapidiaAnalyzer>();
            searchers = new HashMap<Language, IndexSearcher>();
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language, new File(file, language.getLangCode()));
                analyzers.put(language, analyzer);
                searchers.put(language, analyzer.getIndexSearcher());
            }
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
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
        return search(O.PLAINTEXT_FIELD_NAME, searchString, language);
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
        if (!analyzers.containsKey(language)) {
            throw new WikapidiaException("This Analyzer does not support " + language.getEnLangName());
        }
        try {
            QueryParser parser = new QueryParser(O.MATCH_VERSION, fieldName, analyzers.get(language));
            Query query = parser.parse(searchString);
            return searchers.get(language).search(query, HIT_COUNT).scoreDocs;
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "Unable to parse " + searchString + " in " + language.getEnLangName());
            return null;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
