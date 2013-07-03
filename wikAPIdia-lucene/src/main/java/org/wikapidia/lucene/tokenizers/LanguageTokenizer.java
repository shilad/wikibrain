package org.wikapidia.lucene.tokenizers;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.cz.CzechStemFilter;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.de.GermanLightStemFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;
import org.wikapidia.lucene.LuceneOptions;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Ari Weiland
 *
 * This class is based on a class of the same name from Brent Hecht, WikAPIdia.
 * I have updated everything to properly function consistently with lucene 4.3,
 * as well as adding functionality such as the booleans to determine which filters
 * should be applied.
 *
 * Currently contains 26 Tokenizers
 *
 */
public abstract class LanguageTokenizer {

    private static final String STOP_WORDS = "src/main/resources/stopwords/";
    private static Map<Language, Constructor> tokenizerConstructors;

    protected static final LuceneOptions options = new LuceneOptions(new Configuration());
    protected static final Version MATCH_VERSION = options.MATCH_VERSION;

    protected boolean caseInsensitive = true;
    protected boolean useStopWords = true;
    protected boolean useStem = true;

    public static LanguageTokenizer getLanguageTokenizer(Language language, TokenizerOptions options) throws WikapidiaException {
        try {
            mapTokenizers();
            return (LanguageTokenizer) tokenizerConstructors.get(language).newInstance(options);
        } catch (Exception e) {
            throw new WikapidiaException(e);
        }
    }

    public static void mapTokenizers() throws NoSuchMethodException {
        tokenizerConstructors = new HashMap<Language, Constructor>();
        tokenizerConstructors.put(Language.getByLangCode("en"), EnglishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("de"), GermanTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("fr"), FrenchTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("nl"), DutchTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("it"), ItalianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("pl"), PolishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("es"), SpanishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("ru"), RussianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("ja"), JapaneseTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("pt"), PortugueseTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("zh"), ChineseTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("sv"), SwedishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("uk"), UkrainianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("ca"), CatalanTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("no"), NorwegianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("fi"), FinnishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("cs"), CzechTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("hu"), HungarianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("ko"), KoreanTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("id"), IndonesianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("tr"), TurkishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("ro"), RomanianTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("sk"), SlovakTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("da"), DanishTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("he"), HebrewTokenizer.class.getConstructor(TokenizerOptions.class));
        tokenizerConstructors.put(Language.getByLangCode("lad"), LadinoTokenizer.class.getConstructor(TokenizerOptions.class));
    }

    public abstract TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException;

    public Tokenizer getTokenizer(Reader r) {
        return new StandardTokenizer(MATCH_VERSION, r);
    }

    public LanguageTokenizer(TokenizerOptions select) {
        this.caseInsensitive = select.isCaseInsensitive();
        this.useStopWords = select.doesUseStopWords();
        this.useStem = select.doesUseStem();
    }

    public TokenizerOptions getFilters() {
        TokenizerOptions tokenizerOptions = new TokenizerOptions();
        if (caseInsensitive) tokenizerOptions.caseInsensitive();
        if (useStopWords) tokenizerOptions.useStopWords();
        if (useStem) tokenizerOptions.useStem();
        return tokenizerOptions;
    }

    protected static CharArraySet getStopWordsForNonLuceneLangFromFile(Language language) throws WikapidiaException{
        try{
            String langCode = language.getLangCode();
            String fileName = STOP_WORDS + langCode + ".txt";
            InputStream stream = FileUtils.openInputStream(new File(fileName));
            List<String> stopWords = org.apache.commons.io.IOUtils.readLines(stream);
            CharArraySet charArraySet = new CharArraySet(MATCH_VERSION, 0, false);
            for (String stopWord : stopWords) {
                charArraySet.add(stopWord);
            }
            return charArraySet;
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
