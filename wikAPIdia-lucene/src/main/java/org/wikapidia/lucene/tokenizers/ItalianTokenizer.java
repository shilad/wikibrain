package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.it.ItalianLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.lucene.TokenizerOptions;

import java.util.Arrays;

/**
 * @author Ari Weiland
 */
public class ItalianTokenizer extends LanguageTokenizer {

    private final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
            new CharArraySet(MATCH_VERSION, Arrays.asList(
                    "c", "l", "all", "dall", "dell", "nell", "sull", "coll", "pell",
                    "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"
            ), true));

    public ItalianTokenizer(TokenizerOptions select) {
        super(select);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {

        TokenStream stream = new StandardFilter(MATCH_VERSION, input);
        if (caseInsensitive)
            stream = new LowerCaseFilter(MATCH_VERSION, stream);
        if (useStopWords) {
            stream = new ElisionFilter(stream, DEFAULT_ARTICLES);
            stream = new StopFilter(MATCH_VERSION, stream, ItalianAnalyzer.getDefaultStopSet());
        }
        if (useStem) {
            if (!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new ItalianLightStemFilter(stream);
        }
        return stream;
    }
}
