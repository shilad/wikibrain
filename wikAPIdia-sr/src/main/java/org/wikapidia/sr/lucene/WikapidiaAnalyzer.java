package org.wikapidia.sr.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

import java.io.Reader;

/**
 *
 * This class is almost explicitly copied from Brent Hecht, WikAPIdia.
 *
 */
public class WikapidiaAnalyzer extends Analyzer {

    private final Language language;

    /**
     * If you are going to be processing wikiscript, useWikipediaTokenizer should be true
     * @param langId
     * @throws WikapidiaException
     */
    public WikapidiaAnalyzer(int langId) throws WikapidiaException {

        // make sure we're using the correct English version
        if (Language.getById(langId).equals(Language.getByLangCode("simple"))) {
            language= Language.getByLangCode("en");
        } else {
            language = Language.getById(langId);
        }
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
            tokenizer = new StandardTokenizer(Version.LUCENE_40,r);
        }

        try{
            LanguageSpecificTokenizers.WLanguageTokenizer langTokenizer = LanguageSpecificTokenizers.getWLanguageTokenizer(language);
            TokenStream result = langTokenizer.getTokenStream(tokenizer, CharArraySet.EMPTY_SET, Version.LUCENE_40);
            return new Analyzer.TokenStreamComponents(tokenizer, result);
        } catch(WikapidiaException e) {
            throw new RuntimeException(e);
        }
    }
}
