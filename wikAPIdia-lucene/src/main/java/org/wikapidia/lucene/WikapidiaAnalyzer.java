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
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.tokenizers.LanguageTokenizer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author Ari Weiland
 *
 * This class is based on a class of the same name from Brent Hecht, WikAPIdia.
 * I have updated everything to properly function consistently with lucene 4.3,
 * as well as adding functionality such as the FilterSelects and the getIndexWriter
 * and getIndexSearcher methods.
 *
 */
public class WikapidiaAnalyzer extends Analyzer {

    protected final LuceneOptions O = new LuceneOptions(new Configuration());

    private final Language language;
    private TokenizerOptions options;

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with specified filters.
     * @param language
     * @param options
     * @throws IOException
     */
    public WikapidiaAnalyzer(Language language, TokenizerOptions options) throws IOException {
        this.language = language;
        this.options = options;
    }

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with all filters.
     * @param language
     * @throws IOException
     */
    public WikapidiaAnalyzer(Language language) throws IOException {
        this(language, new TokenizerOptions().useStem().useStopWords().caseInsensitive());
    }

    public TokenizerOptions getSelect() {
        return options;
    }

    public void setSelect(TokenizerOptions options) {
        this.options = options;
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
            tokenizer = new StandardTokenizer(O.MATCH_VERSION,r);
        }

        try{
            LanguageTokenizer langTokenizer = LanguageTokenizer.getLanguageTokenizer(language, options);
            TokenStream result = langTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET);
            return new Analyzer.TokenStreamComponents(tokenizer, result);
        } catch (WikapidiaException e) {
            throw new RuntimeException(e);
        }
    }
}
