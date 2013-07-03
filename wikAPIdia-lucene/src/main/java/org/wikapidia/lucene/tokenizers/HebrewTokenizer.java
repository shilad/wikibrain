package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class HebrewTokenizer extends LanguageTokenizer {

    private static CharArraySet stopWords = null;

    public HebrewTokenizer(TokenizerOptions select) {
        super(select);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
        if (stopWords == null){
            stopWords = LanguageTokenizer.getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("he"));
        }
        TokenStream stream = input;
        if (useStopWords)
            stream = new StopFilter(MATCH_VERSION, input, stopWords);
        return stream;
    }
}
