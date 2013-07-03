package org.wikapidia.lucene;

import com.typesafe.config.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneIndexer {

    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString("lucene.version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString("lucene.localId");
    public static final String LANG_ID_FIELD_NAME = conf.get().getString("lucene.langId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString("lucene.wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString("lucene.plaintext");

    private final Directory directory;
    private final Map<Language, WikapidiaAnalyzer> analyzers;
    private final Map<Language, IndexWriter> writers;
    private final Collection<NameSpace> nameSpaces;

    public LuceneIndexer(LanguageSet languages, Collection<NameSpace> nameSpaces) throws WikapidiaException {
        try {
            directory = FSDirectory.open(new File(
                    conf.get().getString("lucene.directory")));
            analyzers = new HashMap<Language, WikapidiaAnalyzer>();
            writers = new HashMap<Language, IndexWriter>();
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language);
                analyzers.put(language, analyzer);
                writers.put(language, analyzer.getIndexWriter(directory));
            }
            this.nameSpaces = nameSpaces;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }

    public void indexPage(RawPage page) throws WikapidiaException {
        Language language = page.getLang();
        if (nameSpaces.contains(page.getNamespace()) && analyzers.containsKey(language)) {
            try {
                IndexWriter writer = writers.get(language);
                Document document = new Document();
                Field localIdField = new IntField(LOCAL_ID_FIELD_NAME, page.getPageId(), Field.Store.YES);
                Field langIdField = new IntField(LANG_ID_FIELD_NAME, page.getLang().getId(), Field.Store.YES);
                Field wikiTextField = new TextField(WIKITEXT_FIELD_NAME, page.getBody(), Field.Store.YES);
                Field plainTextField = new TextField(PLAINTEXT_FIELD_NAME, page.getPlainText(), Field.Store.YES);
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
