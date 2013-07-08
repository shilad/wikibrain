package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.cn.smart.WordTokenFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.LuceneException;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class ChineseTokenizer extends LanguageTokenizer{

    protected ChineseTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws LuceneException {
        TokenStream stream = new WordTokenFilter(input); // breaks Sentences into words
        // stream = new LowerCaseFilter(stream);
        // LowerCaseFilter is not needed, as SegTokenFilter lowercases Basic Latin text.
        // The porter stemming is too strict, this is not a bug, this is a feature:)
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, SmartChineseAnalyzer.getDefaultStopSet());
        if (useStem)
            stream = new PorterStemFilter(stream);
        return stream;
    }
}
