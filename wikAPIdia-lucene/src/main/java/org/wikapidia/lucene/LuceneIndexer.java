package org.wikapidia.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.wikapidia.conf.Configuration;
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

    protected final LuceneOptions O = new LuceneOptions(new Configuration());

    private final File file;
    private final Map<Language, WikapidiaAnalyzer> analyzers;
    private final Map<Language, IndexWriter> writers;
    private final Collection<NameSpace> nameSpaces;

    /**
     * Constructs a LuceneIndexer that will index any RawPage within a
     * specified LanguageSet and a Collection of NameSpaces.
     * @param languages
     * @param nameSpaces
     * @throws WikapidiaException
     */
    public LuceneIndexer(LanguageSet languages, Collection<NameSpace> nameSpaces) throws WikapidiaException {
        try {
            file = O.LUCENE_ROOT;
            Directory directory = new
            analyzers = new HashMap<Language, WikapidiaAnalyzer>();
            writers = new HashMap<Language, IndexWriter>();
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language);
                analyzers.put(language, analyzer);
                IndexWriterConfig iwc = new IndexWriterConfig(O.MATCH_VERSION, analyzer);
                writers.put(language, new IndexWriter(directory, iwc));
            }
            this.nameSpaces = nameSpaces;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }

    /**
     * Indexes a specific RawPage
     * @param page
     * @throws WikapidiaException
     */
    public void indexPage(RawPage page) throws WikapidiaException {
        Language language = page.getLang();
        if (nameSpaces.contains(page.getNamespace()) && analyzers.containsKey(language)) {
            try {
                IndexWriter writer = writers.get(language);
                Document document = new Document();
                Field localIdField = new IntField(O.LOCAL_ID_FIELD_NAME, page.getPageId(), Field.Store.YES);
                Field langIdField = new IntField(O.LANG_ID_FIELD_NAME, page.getLang().getId(), Field.Store.YES);
                Field wikiTextField = new TextField(O.WIKITEXT_FIELD_NAME, page.getBody(), Field.Store.YES);
                Field plainTextField = new TextField(O.PLAINTEXT_FIELD_NAME, page.getPlainText(), Field.Store.YES);
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
