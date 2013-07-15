package org.wikapidia.lucene;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * This class is used to index raw pages during the load process.
 *
 * @author Ari Weiland
 *
 */
public class LuceneIndexer {

    private final File root;
    private final Map<Language, IndexWriter> writers;
    private final LuceneOptions options;
    private final TextFieldBuilder builder;
    private boolean closed = false;

    /**
     * Constructs a LuceneIndexer that will index any RawPage within a
     * specified LanguageSet. Indexes are then placed in language-specific
     * subdirectories in the specified file.
     *
     * @param languages the language set in which this searcher can operate
     * @param root the root directory in which to save all the lucene directories
     */
    public LuceneIndexer(LanguageSet languages, File root) throws ConfigurationException {
        this(languages, root, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a LuceneIndexer that will index any RawPage within a
     * specified LanguageSet. Indexes are then placed in language-specific
     * subdirectories specified by options.
     *
     * @param languages the language set in which this searcher can operate
     * @param options a LuceneOptions object containing specific options for lucene
     */
    public LuceneIndexer(LanguageSet languages, LuceneOptions options) throws ConfigurationException {
        this(languages, options.luceneRoot, options);
    }

    private LuceneIndexer(LanguageSet languages, File root, LuceneOptions options) throws ConfigurationException {
        try {
            this.root = root;
            writers = new HashMap<Language, IndexWriter>();
            for (Language language : languages) {
                WikapidiaAnalyzer analyzer = new WikapidiaAnalyzer(language, options);
                Directory directory = FSDirectory.open(new File(root, language.getLangCode()));
                IndexWriterConfig iwc = new IndexWriterConfig(options.matchVersion, analyzer);
                writers.put(language, new IndexWriter(directory, iwc));
            }
            this.options = options;
            this.builder = new TextFieldBuilder(
                    options.configurator.get(LocalPageDao.class),
                    options.configurator.get(RawPageDao.class),
                    options.configurator.get(RedirectDao.class));
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
     * Clears all data for the languages from the file system
     */
    public void clearIndexes() {
        for (Language language : writers.keySet()) {
            File lang = new File(options.luceneRoot, language.getLangCode());
            if (lang.exists()) {
                try {
                    FileUtils.forceDelete(lang);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Indexes a specific RawPage
     *
     * @param page the page to index
     */
    public void indexPage(RawPage page) throws DaoException {
        if (closed) {
            throw new IllegalStateException("Indexer has already been closed!");
        }
        Language language = page.getLanguage();
        if (getLanguageSet().containsLanguage(language)) {
            try {
                IndexWriter writer = writers.get(language);
                Document document = new Document();
                Field localIdField = new IntField(LuceneOptions.LOCAL_ID_FIELD_NAME, page.getLocalId(), Field.Store.YES);
                Field langIdField = new IntField(LuceneOptions.LANG_ID_FIELD_NAME, page.getLanguage().getId(), Field.Store.YES);
                Field plainTextField = builder.buildTextField(page, options.elements);
                document.add(localIdField);
                document.add(langIdField);
                document.add(plainTextField);
                writer.addDocument(document);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Method should be called when done indexing.
     */
    public void close() {
        closed = true;
        for (IndexWriter writer : writers.values()) {
            IOUtils.closeQuietly(writer);
        }
    }
}
