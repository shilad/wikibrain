package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;

import java.io.Reader;

/**
 * @author Ari Weiland
 */
public class SlovakTokenizer extends LanguageTokenizer {

    private static CharArraySet stopWords = null;

    protected SlovakTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public Tokenizer getTokenizer(Reader r) {
        return new ICUTokenizer(r);
    }

    @Override
    public TokenStream getTokenStream(Reader reader, CharArraySet stemExclusionSet) {
        if (stopWords == null){
            stopWords = getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("sk"));
        }
        TokenStream stream = getTokenizer(reader);
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, stopWords);
        return stream;
    }
}
