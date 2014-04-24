package org.wikibrain.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.cn.smart.WordTokenFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;

import java.io.Reader;

/**
 * @author Ari Weiland
 */
public class ChineseTokenizer extends LanguageTokenizer{

    protected ChineseTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public Tokenizer makeTokenizer(Reader r) {
        return new SentenceTokenizer(r);
    }

    @Override
    public TokenStream getTokenStream(Tokenizer tokenizer, CharArraySet stemExclusionSet) {
        TokenStream stream = new WordTokenFilter(tokenizer); // breaks Sentences into words
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
