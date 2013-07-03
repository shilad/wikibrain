package org.wikapidia.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import static org.wikapidia.lucene.LuceneUtils.*;

/**
 *
 * @author Ari Weiland
 *
 * This class is based on a class of the same name from Brent Hecht, WikAPIdia.
 * I have updated everything to properly function consistently with lucene 4.3,
 * as well as adding functionality such as the FilterSelects and the getIndexWriter
 * and getIndexSearcher methods.
 *
 */public class WikapidiaAnalyzer extends Analyzer {

    private final Language language;
    private final Directory dir;
    private FilterSelect select;

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with specified filters.
     * The WikapidiaAnalyzer stores its lucene files in the specified directory
     * @param language
     * @param file
     * @param select
     * @throws IOException
     */
    public WikapidiaAnalyzer(Language language, File file, FilterSelect select) throws IOException {
        this.language = language;
        this.dir = FSDirectory.open(file);
        this.select = select;
    }

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with all filters.
     * The WikapidiaAnalyzer stores its lucene files in the specified directory
     * @param language
     * @param file
     * @throws IOException
     */
    public WikapidiaAnalyzer(Language language, File file) throws IOException {
        this(language, file, new FilterSelect().useStem().useStopWords().caseInsensitive());
    }

    public FilterSelect getSelect() {
        return select;
    }

    public void setSelect(FilterSelect select) {
        this.select = select;
    }

    public IndexWriter getIndexWriter() throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(MATCH_VERSION, this);
        return new IndexWriter(dir, iwc);
    }

    public IndexSearcher getIndexSearcher() throws IOException {
        DirectoryReader reader = DirectoryReader.open(dir);
        return new IndexSearcher(reader);
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s, Reader r) {
        Tokenizer tokenizer;
        String langCode = language.getLangCode();
        // make sure we're using the correct English version
        if (langCode == "simple") {
            langCode = "en";
        }
        if (langCode.equals("ja")) {
            tokenizer = new JapaneseTokenizer(r, null, false, JapaneseTokenizer.DEFAULT_MODE);
        } else if (langCode.equals("zh")) {
            tokenizer = new SentenceTokenizer(r);
        } else if (langCode.equals("he") || langCode.equals("sk")) {
            tokenizer = new ICUTokenizer(r);
        } else {
            tokenizer = new StandardTokenizer(MATCH_VERSION,r);
        }

        try{
            LanguageSpecificTokenizers.WLanguageTokenizer langTokenizer = LanguageSpecificTokenizers.getWLanguageTokenizer(language);
            langTokenizer.setFilters(select);
            TokenStream result = langTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET);
            return new Analyzer.TokenStreamComponents(tokenizer, result);
        } catch (WikapidiaException e) {
            throw new RuntimeException(e);
        }
    }
}
