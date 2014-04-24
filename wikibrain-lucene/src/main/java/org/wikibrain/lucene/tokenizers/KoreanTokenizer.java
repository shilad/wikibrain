package org.wikibrain.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class KoreanTokenizer extends LanguageTokenizer {

    protected KoreanTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public TokenStream getTokenStream(Tokenizer tokenizer, CharArraySet stemExclusionSet) {
        TokenStream stream = new CJKWidthFilter(tokenizer);
        stream = new CJKBigramFilter(stream);
        if (caseInsensitive)
            stream = new LowerCaseFilter(matchVersion, stream);
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, CJKAnalyzer.getDefaultStopSet());
        return stream;
    }
}
