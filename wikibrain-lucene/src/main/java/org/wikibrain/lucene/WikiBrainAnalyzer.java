package org.wikibrain.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.tokenizers.LanguageTokenizer;

import java.io.Reader;

/**
 *
 * This class is based on a class of the same name from Brent Hecht, WikiBrain.
 * I have updated everything to properly function consistently with lucene 4.3.
 *
 * This class functions as a Lucene Analyzer for a specific language. It runs
 * off of the functions built into the LanguageTokenizer class.
 *
 * TODO: add language overrides for unsupported languages?
 * In other words, analyze language X as similar language Y
 * ie. Ukrainian -&gt; Russian and Ladino -&gt; Spanish
 *
 * @author Ari Weiland
 *
 */
public class WikiBrainAnalyzer extends Analyzer {

    private final Language language;
    private final LanguageTokenizer languageTokenizer;
    private final LuceneOptions options;

    /**
     * Constructs a WikiBrainAnalyzer for the specified language with all filters
     * and default options.
     *
     * @param language the language this analyzer analyzes
     */
    public WikiBrainAnalyzer(Language language) {
        this(language, LuceneOptions.getDefaultOptions());
    }

    /**
     * Constructs a WikiBrainAnalyzer for the specified language with specified filters
     * and specified options.
     *
     * @param language the language this analyzer analyzes
     * @param options a LuceneOptions object containing specific options for lucene
     */
    public WikiBrainAnalyzer(Language language, LuceneOptions options) {
        this.language = language;
        this.languageTokenizer = LanguageTokenizer.getLanguageTokenizer(language, options);
        this.options = options;
    }

    public Language getLanguage() {
        return language;
    }

    public LuceneOptions getOptions() {
        return options;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s, Reader r) {
        Tokenizer tokenizer = languageTokenizer.makeTokenizer(r);
        TokenStream result = languageTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET);
        return new TokenStreamComponents(tokenizer, result);
    }
}
