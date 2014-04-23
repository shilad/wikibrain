package org.wikibrain.lucene;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.RawPage;

import java.io.Closeable;
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
public class LuceneIndexer implements Closeable {

    private final File root;
    private final Language language;
    private final IndexWriter writer;
    private final LuceneOptions[] options;
    private final LuceneOptions mainOptions;
    private final TextFieldBuilder builder;
    private boolean closed = false;

    /**
     * Constructs a LuceneIndexer that will index any RawPage in a
     * specified Language. Indexes are then placed in language-specific
     * subdirectories in the specified file.
     *
     * @param language the language in which this searcher can operate
     * @param root the root directory in which to save all the lucene directories
     */
    public LuceneIndexer(Language language, File root) throws ConfigurationException {
        this(language, root, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a LuceneIndexer that will index a RawPage in the
     * specified language. Indexes are then placed in language-specific
     * subdirectories specified by the first element in options.
     *
     * @param language the language in which this searcher can operate
     * @param options an array of LuceneOptions objects. There must be at least one specified.
     */
    public LuceneIndexer(Language language, LuceneOptions... options) throws ConfigurationException {
        this(language, options[0].luceneRoot, options);
    }

    private LuceneIndexer(Language language, File root, LuceneOptions... options) throws ConfigurationException {
        try {
            this.root = root;
            this.language = language;
            this.options = options;
            this.mainOptions = options[0];
            this.builder = new TextFieldBuilder(
                    mainOptions.configurator.get(LocalPageDao.class),
                    mainOptions.configurator.get(RawPageDao.class),
                    mainOptions.configurator.get(RedirectDao.class));


                File langRoot = new File(root, language.getLangCode());
                if (langRoot.exists()) {
                    FileUtils.deleteQuietly(langRoot);
                }
                WikiBrainAnalyzer analyzer = new WikiBrainAnalyzer(language, mainOptions);
                Directory directory = FSDirectory.open(langRoot);
                IndexWriterConfig iwc = new IndexWriterConfig(mainOptions.matchVersion, analyzer);
                writer = new IndexWriter(directory, iwc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getRoot() {
        return root;
    }

    public LuceneOptions getOptions() {
        return mainOptions;
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
        if (!language.equals(page.getLanguage())) {
            throw new IllegalStateException("Language mismatch!");
        }
        try {
            Document document = new Document();
            Field localIdField = new IntField(LuceneOptions.LOCAL_ID_FIELD_NAME, page.getLocalId(), Field.Store.YES);
            Field langIdField = new IntField(LuceneOptions.LANG_ID_FIELD_NAME, page.getLanguage().getId(), Field.Store.YES);
            Field canonicalTitleField = builder.buildTextField(page, new TextFieldElements().addTitle());
            document.add(localIdField);
            document.add(langIdField);
            document.add(canonicalTitleField);
            if (!page.isRedirect()) {
                for (LuceneOptions option : options) {
                    document.add(builder.buildTextField(page, option.elements));
                }
            }
            writer.addDocument(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method should be called when done indexing.
     */
    public void close() {
        closed = true;
        IOUtils.closeQuietly(writer);
    }
}
