package org.wikapidia.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

import java.io.IOException;
import java.io.Reader;

/**
 *
 * This class is almost explicitly copied from Brent Hecht, WikAPIdia.
 *
 */
public class WikapidiaAnalyzer extends Analyzer {

    private static final String CONF_PATH = "sr.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));

    private final Language language;

    /**
     * If you are going to be processing wikiscript, useWikipediaTokenizer should be true
     * @param language
     * @throws WikapidiaException
     */
    public WikapidiaAnalyzer(Language language) throws WikapidiaException {
        // make sure we're using the correct English version
        if (language.equals(Language.getByLangCode("simple"))) {
            this.language = Language.getByLangCode("en");
        } else {
            this.language = language;
        }
    }

    public IndexWriter getIndexWriter(Directory dir) throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(MATCH_VERSION, this);
        return new IndexWriter(dir, iwc);
    }

    public IndexSearcher getIndexSearcher(Directory dir) throws IOException {
        DirectoryReader reader = DirectoryReader.open(dir);
        return new IndexSearcher(reader);
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s, Reader r) {
        Tokenizer tokenizer = null;
        String langCode = language.getLangCode();
        if (langCode.equals("ja")){
            tokenizer = new JapaneseTokenizer(r, null, false, JapaneseTokenizer.DEFAULT_MODE);
        } else if (langCode.equals("zh")){
            tokenizer = new SentenceTokenizer(r);
        } else if (langCode.equals("he") || langCode.equals("sk")){
            tokenizer = new ICUTokenizer(r);
        } else{
            tokenizer = new StandardTokenizer(MATCH_VERSION,r);
        }

        try{
            LanguageSpecificTokenizers.WLanguageTokenizer langTokenizer = LanguageSpecificTokenizers.getWLanguageTokenizer(language);
            TokenStream result = langTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET);
            return new Analyzer.TokenStreamComponents(tokenizer, result);
        } catch(WikapidiaException e) {
            throw new RuntimeException(e);
        }
    }
}
