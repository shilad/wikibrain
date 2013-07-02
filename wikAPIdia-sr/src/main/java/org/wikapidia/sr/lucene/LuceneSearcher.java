package org.wikapidia.sr.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneSearcher {

    public static final int HIT_COUNT = 1000;

    public static final String CONF_PATH = "parser.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "localId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "plaintext");

    private Analyzer analyzer;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    public LuceneSearcher() throws IOException {
        analyzer = new StandardAnalyzer(MATCH_VERSION);
        reader = DirectoryReader.open(FSDirectory.open(new File(
                conf.get().getString(CONF_PATH + "directory"))));
        searcher = new IndexSearcher(reader);
    }

    public List<ScoredArticle> search(String fieldName, String search) throws ParseException, IOException {
        QueryParser parser = new QueryParser(MATCH_VERSION, fieldName, analyzer);
        Query query = parser.parse(search);
        ScoreDoc[] scoreDocs = searcher.search(query, HIT_COUNT).scoreDocs;
        List<ScoredArticle> scoredArticles = new ArrayList<ScoredArticle>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            scoredArticles.add(new ScoredArticle(
                    searcher.doc(scoreDoc.doc),
                    scoreDoc
            ));
        }
        return scoredArticles;
    }
}
