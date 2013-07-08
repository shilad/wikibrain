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
import org.apache.lucene.util.Version;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.LuceneException;
import org.wikapidia.lucene.TokenizerOptions;

import java.io.IOException;

/**
 * @author Ari Weiland
 */
public class PolishTokenizer extends LanguageTokenizer {

    private static StempelStemmer stemmer;

    protected PolishTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws LuceneException {
        try{
            if (stemmer == null) {
                stemmer = new StempelStemmer(StempelStemmer.load(PolishAnalyzer.class.getResourceAsStream("stemmer_20000.tbl")));
            }
            TokenStream stream = new StandardFilter(matchVersion, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(matchVersion, stream);
            if (useStopWords)
                stream = new StopFilter(matchVersion, stream, PolishAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new StempelFilter(stream, stemmer);
            }
            return stream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
