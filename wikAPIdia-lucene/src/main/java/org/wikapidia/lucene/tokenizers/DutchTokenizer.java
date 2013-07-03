package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.DutchStemmer;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class DutchTokenizer extends LanguageTokenizer {

    public DutchTokenizer(TokenizerOptions select) {
        super(select);
    }

//    static final CharArrayMap<String> DEFAULT_STEM_DICT;
//    static {
//        DEFAULT_STEM_DICT = new CharArrayMap<String>(MATCH_VERSION, 4, false);
//        DEFAULT_STEM_DICT.put("fiets", "fiets"); //otherwise fiet
//        DEFAULT_STEM_DICT.put("bromfiets", "bromfiets"); //otherwise bromfiet
//        DEFAULT_STEM_DICT.put("ei", "eier");
//        DEFAULT_STEM_DICT.put("kind", "kinder");
//    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
        TokenStream stream = new StandardFilter(MATCH_VERSION, input);
        if (caseInsensitive)
            stream = new LowerCaseFilter(MATCH_VERSION, stream);
        if (useStopWords)
            stream = new StopFilter(MATCH_VERSION, stream, DutchAnalyzer.getDefaultStopSet());
        if (useStem) {
            if (!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
//                stream = new StemmerOverrideFilter(stream, DEFAULT_STEM_DICT); // TODO: Dafuq
            stream = new SnowballFilter(stream, new DutchStemmer());
        }
        return stream;
    }
}
