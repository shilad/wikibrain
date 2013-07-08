package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class SlovakTokenizer extends LanguageTokenizer {

    private static CharArraySet stopWords = null;

    public SlovakTokenizer(Version version, TokenizerOptions options) {
        super(version, options);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
        if (stopWords == null){
            stopWords = getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("sk"));
        }
        TokenStream stream = input;
        if (useStopWords)
            stream = new StopFilter(matchVersion, input, stopWords);
        return stream;
    }
}
