package org.wikapidia.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
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
 * This class is used to index raw pages during the load process.
 *
 */
public class LuceneIndexer {

    private final File root;
    private final Map<Language, IndexWriter> writers;
    private final Collection<NameSpace> nameSpaces;
    private final LuceneOptions options;

    /**
     * Constructs a LuceneIndexer that will index any RawPage within a
     * specified LanguageSet and a Collection of NameSpaces. Indexes are
     * then placed in language-specific subdirectories in the specified file.
     * @param languages the language set in which this searcher can operate
     * @param namespaces the namespaces to index
     * @param root the root directory in which to save all the lucene directories
     */
    public LuceneIndexer(LanguageSet languages, Collection<NameSpace> namespaces, File root) {
        this(languages, namespaces, root, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a LuceneIndexer that will index any RawPage within a
     * specified LanguageSet. Indexes are then placed in language-specific
     * subdirectories specified by options.
     * @param languages the language set in which this searcher can operate
     * @param options a LuceneOptions object containing specific options for lucene
     */
    public LuceneIndexer(LanguageSet languages, LuceneOptions options) {
        this(languages, options.namespaces, options.luceneRoot, options);
    }

    private LuceneIndexer(LanguageSet languages, Collection<NameSpace> nameSpaces, File root, LuceneOptions options) {
        try {
            this.root = root;
            writers = new HashMap<Language, IndexWriter>();
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language, options);
                Directory directory = FSDirectory.open(new File(root, language.getLangCode()));
                IndexWriterConfig iwc = new IndexWriterConfig(options.matchVersion, analyzer);
                writers.put(language, new IndexWriter(directory, iwc));
            }
            this.nameSpaces = nameSpaces;
            this.options = options;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getRoot() {
        return root;
    }

    public LanguageSet getLanguageSet() {
        return new LanguageSet(writers.keySet());
    }

    public LuceneOptions getOptions() {
        return options;
    }

    /**
     * Indexes a specific RawPage
     * @param page the page to index
     */
    public void indexPage(RawPage page) {
        Language language = page.getLang();
        if (nameSpaces.contains(page.getNamespace()) && getLanguageSet().containsLanguage(language)) {
            try {
                IndexWriter writer = writers.get(language);
                Document document = new Document();
                Field localIdField = new IntField(LuceneOptions.LOCAL_ID_FIELD_NAME, page.getPageId(), Field.Store.YES);
                Field langIdField = new IntField(LuceneOptions.LANG_ID_FIELD_NAME, page.getLang().getId(), Field.Store.YES);
                Field wikiTextField = new TextField(LuceneOptions.WIKITEXT_FIELD_NAME, page.getBody(), Field.Store.YES);
                Field plainTextField = new TextField(LuceneOptions.PLAINTEXT_FIELD_NAME, page.getPlainText(), Field.Store.YES);
                document.add(localIdField);
                document.add(langIdField);
                document.add(wikiTextField);
                document.add(plainTextField);
                writer.addDocument(document);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
