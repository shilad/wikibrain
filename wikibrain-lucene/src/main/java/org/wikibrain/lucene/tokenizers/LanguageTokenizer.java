package org.wikibrain.lucene.tokenizers;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;
import org.wikibrain.lucene.LuceneOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * This class is based on a class of the same name from Brent Hecht, WikiBrain.
 * I have updated everything to properly function consistently with Lucene 4.3.
 *
 * This class is used to generate Tokenizers for specific languages. It allows for
 * specifying different types of filters to apply to the child Tokenizers.
 *
 * There are currently 35 language-specific tokenizer subclasses, plus a
 * DefaultTokenizer that will do its best on all other languages.
 * Note that simple English is treated as standard English
 *
 * @author Ari Weiland
 *
 */
public abstract class LanguageTokenizer {

    private static final String STOP_WORDS = "src/main/resources/stopwords/";
    private static Map<Language, Class> tokenizerClasses;

    protected final Version matchVersion;
    protected final boolean caseInsensitive;
    protected final boolean useStopWords;
    protected final boolean useStem;
    protected final Language language;

    protected LanguageTokenizer(Version version, TokenizerOptions tokenizerOptions, Language language) {
        this.matchVersion = version;
        this.caseInsensitive = tokenizerOptions.isCaseInsensitive();
        this.useStopWords = tokenizerOptions.doesUseStopWords();
        this.useStem = tokenizerOptions.doesUseStem();
        this.language = language;
    }

    /**
     * Primary workhorse method of this class. Children will implement this and apply
     * appropriate filters to return a TokenStream.
     *
     * @param tokenizer
     * @param stemExclusionSet
     * @return
     */
    public abstract TokenStream getTokenStream(Tokenizer tokenizer, CharArraySet stemExclusionSet);

    public Tokenizer makeTokenizer(Reader r) {
        return new StandardTokenizer(matchVersion, r);
    }

    public TokenStream getTokenStream(Reader r) {
        return getTokenStream(makeTokenizer(r), CharArraySet.EMPTY_SET);
    }

    public TokenizerOptions getTokenizerOptions() {
        TokenizerOptions options = new TokenizerOptions();
        if (caseInsensitive) options.caseInsensitive();
        if (useStopWords) options.useStopWords();
        if (useStem) options.useStem();
        return options;
    }

    public Language getLanguage() {
        return language;
    }

    /**
     * Returns an instance of a LanguageTokenizer for the specified language
     * with the filters specified by opts.
     *
     * @param language the language of the tokenizer to be retrieved
     * @param opts the LuceneOptions object
     * @return a LanguageTokenizer for language configured by opts
     */
    public static LanguageTokenizer getLanguageTokenizer(Language language, LuceneOptions opts) {
        try {
            if (language.equals(Language.getByLangCode("simple"))) language = Language.getByLangCode("en"); // simple english
            if (tokenizerClasses.containsKey(language)) {                                                   // is just english
                return (LanguageTokenizer) tokenizerClasses.get(language)
                        .getDeclaredConstructor(
                                Version.class,
                                TokenizerOptions.class,
                                Language.class)
                        .newInstance(
                                opts.matchVersion,
                                opts.options,
                                language);
            } else {
                return new DefaultTokenizer(
                        opts.matchVersion,
                        opts.options,
                        language);
            }
        } catch (Exception e) {
            throw new RuntimeException(e); // These exceptions are based on hard code and should never get thrown
        }
    }

    /**
     * Returns an instance of a LanguageTokenizer for the specified language
     * with the filters specified by opts.
     *
     * @param language the language of the tokenizer to be retrieved
     * @param opts the LuceneOptions object
     * @return a LanguageTokenizer for language configured by opts
     */
    public static LanguageTokenizer getLanguageTokenizer(Language language, TokenizerOptions opts, Version version) {
        try {
            if (language.equals(Language.getByLangCode("simple"))) language = Language.getByLangCode("en"); // simple english
            if (tokenizerClasses.containsKey(language)) {                                                   // is just english
                return (LanguageTokenizer) tokenizerClasses.get(language)
                        .getDeclaredConstructor(
                                Version.class,
                                TokenizerOptions.class,
                                Language.class)
                        .newInstance(
                                version,
                                opts,
                                language);
            } else {
                return new DefaultTokenizer(
                        version,
                        opts,
                        language);
            }
        } catch (Exception e) {
            throw new RuntimeException(e); // These exceptions are based on hard code and should never get thrown
        }
    }

    static {
        tokenizerClasses = new HashMap<Language, Class>();

        // These 26 tokenizers are functionally identical to Brent's code,
        // except for Dutch (nl), which I modified a good deal
        tokenizerClasses.put(Language.getByLangCode("en"), EnglishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("de"), GermanTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("fr"), FrenchTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("nl"), DutchTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("it"), ItalianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("pl"), PolishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("es"), SpanishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("ru"), RussianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("ja"), JapaneseTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("pt"), PortugueseTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("zh"), ChineseTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("sv"), SwedishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("uk"), UkrainianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("ca"), CatalanTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("no"), NorwegianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("fi"), FinnishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("cs"), CzechTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("hu"), HungarianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("ko"), KoreanTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("id"), IndonesianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("tr"), TurkishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("ro"), RomanianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("sk"), SlovakTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("da"), DanishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("he"), HebrewTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("lad"), LadinoTokenizer.class);

        // I have added these 9 tokenizers myself
        tokenizerClasses.put(Language.getByLangCode("ar"), ArabicTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("bg"), BulgarianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("el"), GreekTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("eu"), BasqueTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("ga"), IrishTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("gl"), GalicianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("hi"), HindiTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("hy"), ArmenianTokenizer.class);
        tokenizerClasses.put(Language.getByLangCode("lv"), LatvianTokenizer.class);

        // The following two tokenizers are of questionable functionality
        // and are not currently implemented
//        tokenizerClasses.put(Language.getByLangCode("fa"), PersianTokenizer.class);
//        tokenizerClasses.put(Language.getByLangCode("th"), ThaiTokenizer.class);

    }

    protected static CharArraySet getStopWordsForNonLuceneLangFromFile(Version version, Language language) {
        try{
            String langCode = language.getLangCode();
            String fileName = STOP_WORDS + langCode + ".txt";
            CharArraySet charArraySet = new CharArraySet(version, 0, false);
            File stopWordsFile = new File(fileName);
            if (stopWordsFile.exists()) {
                InputStream stream = FileUtils.openInputStream(new File(fileName));
                List<String> stopWords = org.apache.commons.io.IOUtils.readLines(stream);
                for (String stopWord : stopWords) {
                    charArraySet.add(stopWord);
                }
            }
            return charArraySet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
