package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;

import java.io.Reader;

/**
 * @author Ari Weiland
 */
public class LatvianTokenizer extends LanguageTokenizer {

    protected LatvianTokenizer(Version version, TokenizerOptions tokenizerOptions, Language language) {
        super(version, tokenizerOptions, language);
    }

    @Override
    public TokenStream getTokenStream(Reader reader, CharArraySet stemExclusionSet) {
        TokenStream stream = getTokenizer(reader);
        stream = new StandardFilter(matchVersion, stream);
        if (caseInsensitive)
            stream = new LowerCaseFilter(matchVersion, stream);
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, LatvianAnalyzer.getDefaultStopSet());
        if (useStem) {
            if (!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new LatvianStemFilter(stream);
        }
        return stream;
    }
}
