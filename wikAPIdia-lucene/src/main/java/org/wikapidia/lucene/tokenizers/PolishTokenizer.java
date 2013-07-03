package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.stempel.StempelFilter;
import org.apache.lucene.analysis.stempel.StempelStemmer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.lucene.TokenizerOptions;

import java.io.IOException;

/**
 * @author Ari Weiland
 */
public class PolishTokenizer extends LanguageTokenizer {

    private static StempelStemmer stemmer;

    public PolishTokenizer(TokenizerOptions select) {
        super(select);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
        try{
            if (stemmer == null) {
                stemmer = new StempelStemmer(StempelStemmer.load(PolishAnalyzer.class.getResourceAsStream("stemmer_20000.tbl")));
            }
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, PolishAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new StempelFilter(stream, stemmer);
            }
            return stream;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
