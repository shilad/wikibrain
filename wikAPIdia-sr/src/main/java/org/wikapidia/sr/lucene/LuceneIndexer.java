package org.wikapidia.sr.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneIndexer {

    public static final String CONF_PATH = "parser.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "localId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "plaintext");

    private Analyzer analyzer;
    private Directory directory;
    private IndexWriterConfig config;
    private IndexWriter writer;

    public LuceneIndexer() throws IOException {
        analyzer = new StandardAnalyzer(MATCH_VERSION); // TODO: Find/write a more specific analyzer?
        directory = FSDirectory.open(new File(
                conf.get().getString(CONF_PATH + "directory")));
        config = new IndexWriterConfig(MATCH_VERSION, analyzer);
        writer = new IndexWriter(directory, config);
    }

    public void indexPage(RawPage page) throws WikapidiaException {
        if (page.getNamespace() == NameSpace.ARTICLE) { // TODO: Is this actually necessary?
            Document document = new Document();
            Field localIdField = new IntField(LOCAL_ID_FIELD_NAME, page.getPageId(), Field.Store.YES);
            Field wikiTextField = new TextField(WIKITEXT_FIELD_NAME, page.getBody(), Field.Store.YES);
            Field plainTextField = new TextField(PLAINTEXT_FIELD_NAME, page.getPlainText(), Field.Store.YES); // should be parsed in WikapidiaAnalyzer
            document.add(localIdField);
            document.add(wikiTextField);
            document.add(plainTextField);
            try {
                writer.addDocument(document);
            } catch (IOException e) {
                throw new WikapidiaException(e);
            }
        }
    }
}
