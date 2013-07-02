package org.wikapidia.sr.utils;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class ESAAnalyzer extends Analyzer {

    public final CharArraySet ENGLISH_STOP_WORDS_SET;

    public ESAAnalyzer() {
        List<String> stopWords = new ArrayList<String>();
        try {
            List<String> lines = IOUtils.readLines(ESAAnalyzer.class.getResourceAsStream("/stopwords.txt"));
            for (String line : lines) {
                if (line != null) {
                    stopWords.add(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        ENGLISH_STOP_WORDS_SET = StopFilter.makeStopSet(Version.LUCENE_43, stopWords);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer wt = new WikipediaTokenizer(reader);
        TokenFilter filter = new LowerCaseFilter(Version.LUCENE_43, wt);
        filter = new StopFilter(Version.LUCENE_43, filter, ENGLISH_STOP_WORDS_SET);
        filter = new SnowballFilter(filter, "English");
        return new TokenStreamComponents(wt, filter);
    }
}
