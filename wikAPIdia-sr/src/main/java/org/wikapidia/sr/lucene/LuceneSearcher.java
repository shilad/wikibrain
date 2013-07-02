package org.wikapidia.sr.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneSearcher {

    public static final int HIT_COUNT = 1000;

    private static final String CONF_PATH = "sr.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "localId");
    public static final String LANG_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "langId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "plaintext");

    private Directory directory;
    private Map<Language, WikapidiaAnalyzer> analyzers;
    private Map<Language, IndexSearcher> searchers;

    public LuceneSearcher(LanguageSet languages) throws WikapidiaException {
        try {
            directory = FSDirectory.open(new File(
                    conf.get().getString(CONF_PATH + "directory")));
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language);
                analyzers.put(language, analyzer);
                searchers.put(language, analyzer.getIndexSearcher(directory));
            }
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }

    public ScoreDoc[] search(String fieldName, String searchString, Language language) throws ParseException, IOException {
        QueryParser parser = new QueryParser(MATCH_VERSION, fieldName, analyzers.get(language));
        Query query = parser.parse(searchString);
        return searchers.get(language).search(query, HIT_COUNT).scoreDocs;
    }

    public List<ScoredArticle> convert(ScoreDoc[] scoreDocs, Language language) throws IOException {
        List<ScoredArticle> scoredArticles = new ArrayList<ScoredArticle>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            scoredArticles.add(new ScoredArticle(
                    searchers.get(language).doc(scoreDoc.doc),
                    scoreDoc
            ));
        }
        return scoredArticles;
    }
}
