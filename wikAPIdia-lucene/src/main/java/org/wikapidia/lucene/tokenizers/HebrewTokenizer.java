package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.LuceneException;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class HebrewTokenizer extends LanguageTokenizer {

    private static CharArraySet stopWords = null;

    protected HebrewTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws LuceneException {
        if (stopWords == null){
            stopWords = LanguageTokenizer.getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("he"));
        }
        TokenStream stream = input;
        if (useStopWords)
            stream = new StopFilter(matchVersion, input, stopWords);
        return stream;
    }
}
