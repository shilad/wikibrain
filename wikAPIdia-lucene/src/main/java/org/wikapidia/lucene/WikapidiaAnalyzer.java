package org.wikapidia.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.tokenizers.LanguageTokenizer;

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

    private final Language language;
    private final TokenizerOptions options;
    private final LuceneOptions opts;

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with all filters
     * and default opts.
     * @param language
     */
    public WikapidiaAnalyzer(Language language) {
        this(
                language,
                new TokenizerOptions().useStem().useStopWords().caseInsensitive(),
                new LuceneOptions());
    }

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with specified filters
     * and default opts.
     * @param language
     * @param options
     */
    public WikapidiaAnalyzer(Language language, TokenizerOptions options) {
        this(
                language,
                options,
                new LuceneOptions());
    }

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with all filters
     * and specified opts.
     * @param language
     * @param opts a LuceneOptions object containing specific options for lucene
     */
    public WikapidiaAnalyzer(Language language, LuceneOptions opts) {
        this(
                language,
                new TokenizerOptions().useStem().useStopWords().caseInsensitive(),
                opts);
    }

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with specified filters
     * and specified opts.
     * @param language
     * @param options
     * @param opts a LuceneOptions object containing specific options for lucene
     */
    public WikapidiaAnalyzer(Language language, TokenizerOptions options, LuceneOptions opts) {
        this.language = language;
        this.options = options;
        this.opts = opts;
    }

    /**
     * Returns the Lucene Options
     * @return
     */
    public LuceneOptions getOpts() {
        return opts;
    }

    public Language getLanguage() {
        return language;
    }

    /**
     * Returns the Tokenizer Options
     * @return
     */
    public TokenizerOptions getOptions() {
        return options;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s, Reader r) {
        Tokenizer tokenizer;
        String langCode = language.getLangCode();
        if (langCode.equals("simple")) langCode = "en"; // simple english is just english
        if (langCode.equals("ja")) {
            tokenizer = new JapaneseTokenizer(r, null, false, JapaneseTokenizer.DEFAULT_MODE);
        } else if (langCode.equals("zh")) {
            tokenizer = new SentenceTokenizer(r);
        } else if (langCode.equals("he") || langCode.equals("sk")) {
            tokenizer = new ICUTokenizer(r);
        } else {
            tokenizer = new StandardTokenizer(opts.matchVersion,r);
        }

        try {
            LanguageTokenizer langTokenizer = LanguageTokenizer.getLanguageTokenizer(language, options, opts);
            TokenStream result = langTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET);
            return new Analyzer.TokenStreamComponents(tokenizer, result);
        } catch (WikapidiaException e) {
            throw new RuntimeException(e);
        }
    }
}
