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
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneIndexer {

    private static final String CONF_PATH = "sr.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "localId");
    public static final String LANG_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "langId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "plaintext");

    private Directory directory;
    private Map<Language, WikapidiaAnalyzer> analyzers;
    private Map<Language, IndexWriter> writers;

    public LuceneIndexer(LanguageSet languages) throws WikapidiaException {
        try {
            directory = FSDirectory.open(new File(
                    conf.get().getString(CONF_PATH + "directory")));
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language);
                analyzers.put(language, analyzer);
                writers.put(language, analyzer.getIndexWriter(directory));
            }
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }

    public void indexPage(RawPage page) throws WikapidiaException {
        Language language = page.getLang();
        // TODO: Is it really necessary to only index articles?
        if (page.getNamespace() == NameSpace.ARTICLE && analyzers.containsKey(language)) {
            try {
                IndexWriter writer = writers.get(language);
                Document document = new Document();
                Field localIdField = new IntField(LOCAL_ID_FIELD_NAME, page.getPageId(), Field.Store.YES);
                Field langIdField = new IntField(LANG_ID_FIELD_NAME, page.getLang().getId(), Field.Store.YES);
                Field wikiTextField = new TextField(WIKITEXT_FIELD_NAME, page.getBody(), Field.Store.YES);
                Field plainTextField = new TextField(PLAINTEXT_FIELD_NAME, page.getPlainText(), Field.Store.YES); // should be parsed in WikapidiaAnalyzer
                document.add(localIdField);
                document.add(langIdField);
                document.add(wikiTextField);
                document.add(plainTextField);
                writer.addDocument(document);
            } catch (IOException e) {
                throw new WikapidiaException(e);
            }
        }
    }
}
