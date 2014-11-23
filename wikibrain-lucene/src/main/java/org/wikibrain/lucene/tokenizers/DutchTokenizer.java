package org.wikibrain.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.DutchStemmer;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class DutchTokenizer extends LanguageTokenizer {

    protected DutchTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

//    static final CharArrayMap<String> DEFAULT_STEM_DICT;
//    static {
//        DEFAULT_STEM_DICT = new CharArrayMap<String>(matchVersion, 4, false);
//        DEFAULT_STEM_DICT.put("fiets", "fiets"); //otherwise fiet
//        DEFAULT_STEM_DICT.put("bromfiets", "bromfiets"); //otherwise bromfiet
//        DEFAULT_STEM_DICT.put("ei", "eier");
//        DEFAULT_STEM_DICT.put("kind", "kinder");
//    }

    @Override
    public TokenStream getTokenStream(Tokenizer tokenizer, CharArraySet stemExclusionSet) {
        TokenStream stream = new StandardFilter(matchVersion, tokenizer);
        if (caseInsensitive)
            stream = new LowerCaseFilter(matchVersion, stream);
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, DutchAnalyzer.getDefaultStopSet());
        if (useStem) {
            if (!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
//                stream = new StemmerOverrideFilter(stream, DEFAULT_STEM_DICT); // TODO: Dafuq
            stream = new SnowballFilter(stream, new DutchStemmer());
        }
        return stream;
    }
}
