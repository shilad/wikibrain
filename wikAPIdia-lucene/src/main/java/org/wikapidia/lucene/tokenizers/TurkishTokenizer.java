package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.TurkishStemmer;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.LuceneException;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class TurkishTokenizer extends LanguageTokenizer {

    protected TurkishTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws LuceneException {
        TokenStream stream = new StandardFilter(matchVersion, input);
        if (caseInsensitive)
            stream = new LowerCaseFilter(matchVersion, stream);
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, TurkishAnalyzer.getDefaultStopSet());
        if (useStem) {
            if (!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new TurkishStemmer());
        }
        return stream;
    }
}
