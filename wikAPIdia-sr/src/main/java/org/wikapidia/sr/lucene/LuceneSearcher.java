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

/**
 *
 * @author
 */
public class LuceneSearcher {

    public static final int HIT_COUNT = 1000;

    public static final String CONF_PATH = "parser.lucene.";
    private Configuration conf = new Configuration(null);

    public final Version matchVersion = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));
    public final String localIdFieldName = conf.get().getString(CONF_PATH + "localId");
    public final String wikitextFieldName = conf.get().getString(CONF_PATH + "wikitext");
    public final String plaintextFieldName = conf.get().getString(CONF_PATH + "plaintext");

    private Analyzer analyzer;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    public LuceneSearcher() throws IOException {
        analyzer = new StandardAnalyzer(matchVersion);
        reader = DirectoryReader.open(FSDirectory.open(new File(
                conf.get().getString(CONF_PATH + "directory"))));
        searcher = new IndexSearcher(reader);
    }

    public ScoreDoc[] search(String fieldName, String search) throws ParseException, IOException {
        QueryParser parser = new QueryParser(matchVersion, fieldName, analyzer);
        Query query = parser.parse(search);
        return searcher.search(query, HIT_COUNT).scoreDocs;
    }
}
