package org.wikapidia.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.tokenizers.LanguageTokenizer;

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
    private final LuceneOptions options;

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with all filters
     * and default options.
     * @param language
     */
    public WikapidiaAnalyzer(Language language) {
        this(language, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a WikapidiaAnalyzer for the specified language with specified filters
     * and specified options.
     * @param language
     * @param options a LuceneOptions object containing specific options for lucene
     */
    public WikapidiaAnalyzer(Language language, LuceneOptions options) {
        this.language = language;
        this.options = options;
    }

    /**
     * Returns the Lucene Options
     * @return
     */
    public LuceneOptions getOptions() {
        return options;
    }

    public Language getLanguage() {
        return language;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s, Reader r) {
        Tokenizer tokenizer;
        String langCode = language.getLangCode();
        if (langCode.equals("ja")) {
            tokenizer = new JapaneseTokenizer(r, null, false, JapaneseTokenizer.DEFAULT_MODE);
        } else if (langCode.equals("zh")) {
            tokenizer = new SentenceTokenizer(r);
        } else if (langCode.equals("he") || langCode.equals("sk")) {
            tokenizer = new ICUTokenizer(r);
        } else {
            tokenizer = new StandardTokenizer(options.matchVersion,r);
        }

        LanguageTokenizer langTokenizer = LanguageTokenizer.getLanguageTokenizer(language, options);
        TokenStream result = langTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET);
        return new TokenStreamComponents(tokenizer, result);
    }
}
