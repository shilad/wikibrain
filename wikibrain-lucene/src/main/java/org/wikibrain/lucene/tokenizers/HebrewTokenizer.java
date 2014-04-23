package org.wikibrain.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;

import java.io.Reader;

/**
 * @author Ari Weiland
 */
public class HebrewTokenizer extends LanguageTokenizer {

    private static CharArraySet stopWords = null;

    protected HebrewTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    public Tokenizer makeTokenizer(Reader r) {
        return new ICUTokenizer(r);
    }

    @Override
    public TokenStream getTokenStream(Tokenizer tokenizer, CharArraySet stemExclusionSet) {
        if (stopWords == null){
            stopWords = LanguageTokenizer.getStopWordsForNonLuceneLangFromFile(matchVersion, Language.getByLangCode("he"));
        }
        TokenStream stream = tokenizer;
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, stopWords);
        return stream;
    }
}
