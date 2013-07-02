package org.wikapidia.lucene;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.cn.smart.WordTokenFilter;
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
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.fr.FrenchLightStemFilter;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.it.ItalianLightStemFilter;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.miscellaneous.StemmerOverrideFilter;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseLightStemFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.stempel.StempelFilter;
import org.apache.lucene.analysis.stempel.StempelStemmer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.analysis.tr.TurkishLowerCaseFilter;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 *
 * This class is almost explicitly copied from Brent Hecht, WikAPIdia.
 *
 */
public class LanguageSpecificTokenizers {

    private static final String STOP_WORDS = "/src/main/resources/stopwords/";
    private static final String CONF_PATH = "sr.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));

    public static WLanguageTokenizer getWLanguageTokenizer(Language language) throws WikapidiaException {
        try{
            String englishName = language.getEnLangName();
            if (language.equals(Language.getByLangCode("no"))) {
                englishName = "Norwegian"; // otherwise would be Norwegian (Bokmal)
            }
            Class<WLanguageTokenizer> wltClass =
                    (Class<WLanguageTokenizer>) Class.forName(LanguageSpecificTokenizers.class.getCanonicalName() + "$" + englishName + "Tokenizer");
            WLanguageTokenizer rVal = wltClass.newInstance();
            return rVal;
        } catch(Exception e) {
            throw new WikapidiaException(e);
        }

    }

    public static abstract class WLanguageTokenizer {

        protected boolean caseInsensitive;
        protected boolean stopWords;
        protected boolean stem;
        protected boolean punctuation;

        protected abstract TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException;

        public Tokenizer getTokenizer(Reader r) {
            return new StandardTokenizer(MATCH_VERSION, r);
        }
    }

    public static class SpanishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive) 
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords) 
                stream = new StopFilter(MATCH_VERSION, stream, SpanishAnalyzer.getDefaultStopSet());
            if (stem && !stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SpanishLightStemFilter(stream);
            return stream;
        }
    }

    public static class HungarianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords)
                stream = new StopFilter(MATCH_VERSION, stream, HungarianAnalyzer.getDefaultStopSet());
            if (stem && !stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new HungarianStemmer());
            return stream;
        }
    }

    public static class PolishTokenizer extends WLanguageTokenizer {

        private static StempelStemmer stemmer;

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            try{
                if (stemmer == null) {
                    stemmer = new StempelStemmer(StempelStemmer.load(PolishAnalyzer.class.getResourceAsStream("stemmer_20000.tbl")));
                }
                TokenStream stream = new StandardFilter(MATCH_VERSION, input);
                if (caseInsensitive)
                    stream = new LowerCaseFilter(MATCH_VERSION, stream);
                if (stopWords)
                    stream = new StopFilter(MATCH_VERSION, stream, PolishAnalyzer.getDefaultStopSet());
                if (stem && !stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new StempelFilter(stream, stemmer);
                return stream;
            } catch(IOException e) {
                throw new WikapidiaException(e);
            }
        }
    }

    public static class IndonesianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords)
                stream = new StopFilter(MATCH_VERSION, stream, IndonesianAnalyzer.getDefaultStopSet());
            if (stem && !stemExclusionSet.isEmpty()) {
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            }
            return stream;
        }
    }

    public static class JapaneseTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new JapaneseBaseFormFilter(input);
            stream = new CJKWidthFilter(stream);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords) {
                stream = new JapanesePartOfSpeechStopFilter(true, stream, JapaneseAnalyzer.getDefaultStopTags());
                stream = new StopFilter(MATCH_VERSION, stream, JapaneseAnalyzer.getDefaultStopSet());
            }
            if (stem)
                stream = new JapaneseKatakanaStemFilter(stream);
            return stream;
        }
    }

    public static class KoreanTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new CJKWidthFilter(input);
            stream = new CJKBigramFilter(stream); // TODO: I don't know what the fuck to do with this! Harrison help me!
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords)
                stream = new StopFilter(MATCH_VERSION, stream, CJKAnalyzer.getDefaultStopSet());
            return stream;
        }
    }

    public static class ItalianTokenizer extends WLanguageTokenizer {

        private final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
                new CharArraySet(MATCH_VERSION, Arrays.asList(
                                "c", "l", "all", "dall", "dell", "nell", "sull", "coll", "pell",
                                "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"
                ), true));

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {

            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords) {
                stream = new ElisionFilter(stream, DEFAULT_ARTICLES);
                stream = new StopFilter(MATCH_VERSION, stream, ItalianAnalyzer.getDefaultStopSet());
            }
            if (stem && !stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new ItalianLightStemFilter(stream);
            return stream;
        }
    }

    public static class DutchTokenizer extends WLanguageTokenizer {

        public final static String DEFAULT_STOPWORD_FILE = "dutch_stop.txt";
        static final CharArraySet DEFAULT_STOP_SET;
        static final CharArrayMap<String> DEFAULT_STEM_DICT;
        static {
            try {
                DEFAULT_STOP_SET = WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class,
                        DEFAULT_STOPWORD_FILE, IOUtils.CHARSET_UTF_8), MATCH_VERSION);
            } catch (IOException ex) {
                // default set should always be present as it is part of the
                // distribution (JAR)
                throw new RuntimeException("Unable to load default stopword set");
            }

            DEFAULT_STEM_DICT = new CharArrayMap<String>(MATCH_VERSION, 4, false);
            DEFAULT_STEM_DICT.put("fiets", "fiets"); //otherwise fiet
            DEFAULT_STEM_DICT.put("bromfiets", "bromfiets"); //otherwise bromfiet
            DEFAULT_STEM_DICT.put("ei", "eier");
            DEFAULT_STEM_DICT.put("kind", "kinder");
        }

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (stopWords)
                stream = new StopFilter(MATCH_VERSION, stream, DutchAnalyzer.getDefaultStopSet() /*DEFAULT_STOP_SET*/);
            if (stem && !stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, new CharArraySet(MATCH_VERSION, 0, false));
            stream = new StemmerOverrideFilter(stream, DEFAULT_STEM_DICT); // TODO: Dafuq
            stream = new SnowballFilter(stream, new org.tartarus.snowball.ext.DutchStemmer());
            return stream;
        }
    }

    public static class NorwegianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, NorwegianAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new NorwegianStemmer());
            return stream;
        }

    }


    /**
     * Directly copied from Spanish (Don't know what to do about the Hebrew, but this
     * is only for testing anyway)
     * @author bjhecht
     *
     */
    public static class LadinoTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            stream = new LowerCaseFilter(MATCH_VERSION, stream);
            stream = new StopFilter(MATCH_VERSION, stream, SpanishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);

            stream = new SpanishLightStemFilter(stream);
            return stream;
        }
    }

    public static class PortugueseTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, PortugueseAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty()){
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            }
            stream = new PortugueseLightStemFilter(stream);
            return stream;

        }
    }

    public static class RomanianTokenizer extends WLanguageTokenizer{


        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, RomanianAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new RomanianStemmer());
            return stream;

        }

    }

    public static class RussianTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, RussianAnalyzer.getDefaultStopSet());
            if (!stemExclusionSet.isEmpty()) stream = new SetKeywordMarkerFilter(
                    stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new org.tartarus.snowball.ext.RussianStemmer());
            return stream;

        }
    }

    /**
     * Just using Russian for Ukrainian for now
     * @author bjhecht
     *
     */
    public static class UkrainianTokenizer extends RussianTokenizer{};

    public static class SwedishTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, SwedishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new SwedishStemmer());
            return stream;

        }

    }

    public static class TurkishTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new TurkishLowerCaseFilter(stream);
            stream = new StopFilter(version, stream, TurkishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new TurkishStemmer());
            return stream;
        }

    }

    public static class ChineseTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new WordTokenFilter(input);
            // stream = new LowerCaseFilter(stream);
            // LowerCaseFilter is not needed, as SegTokenFilter lowercases Basic Latin text.
            // The porter stemming is too strict, this is not a bug, this is a feature:)
            stream = new PorterStemFilter(stream);
            stream = new StopFilter(version, stream, SmartChineseAnalyzer.getDefaultStopSet());
            return stream;
        }


    }

    //	public static class FinnishTokenizer extends WLanguageTokenizer{
    //
    //	}
    //
    public static class HebrewTokenizer extends WLanguageTokenizer{

        private static CharArraySet stopWords = null;

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            if (stopWords == null){
                stopWords = getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("he"));
            }

            TokenStream stream = new StopFilter(MATCH_VERSION, input, stopWords);
            return stream;

        }
    }

    public static class SlovakTokenizer extends WLanguageTokenizer{

        private static CharArraySet stopWords = null;

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            if (stopWords == null){
                stopWords = getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("sk"));
            }

            TokenStream stream = new StopFilter(MATCH_VERSION, input,
                    stopWords);
            return stream;
        }
    }

    public static class EnglishTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet, Version version) throws WikapidiaException{
            TokenStream stream = new StandardFilter(version, input);
            stream = new EnglishPossessiveFilter(version, stream);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, EnglishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty()){
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            }
            stream = new PorterStemFilter(stream);
            return stream;
        }

    }

    public static class DanishTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, DanishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new DanishStemmer());
            return stream;

        }
    }

    public static class FrenchTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new ElisionFilter(stream, FrenchAnalyzer.DEFAULT_ARTICLES);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, FrenchAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);

            stream = new FrenchLightStemFilter(stream);
            return stream;


        }


    }

    public static class FinnishTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            //			final Tokenizer source = new StandardTokenizer(MATCH_VERSION, reader);
            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, FinnishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new FinnishStemmer());
            return stream;

        }


    }

    public static class CzechTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter( version, stream, CzechAnalyzer.getDefaultStopSet());

            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new CzechStemFilter(stream);
            return stream;

        }


    }



    public static class CatalanTokenizer extends WLanguageTokenizer{

        private static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
                new CharArraySet(Version.LUCENE_CURRENT, //note: this LUCENCE_CURRENT is actually in the Lucene 4.0 source code, where much of this code is copied from
                        Arrays.asList(
                                "d", "l", "m", "n", "s", "t"
                        ), true));

        @Override
        protected TokenStream getTokenStream(TokenStream input,
                                                 CharArraySet stemExclusionSet, Version version)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(version, input);
            stream = new ElisionFilter(stream, DEFAULT_ARTICLES);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, DanishAnalyzer.getDefaultStopSet());
            if(!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new CatalanStemmer());
            return stream;

        }
    }

    public static class GermanTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet, Version version) throws WikapidiaException{
            TokenStream stream = new StandardFilter(version, input);
            stream = new LowerCaseFilter(version, stream);
            stream = new StopFilter(version, stream, GermanAnalyzer.getDefaultStopSet());
            stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new GermanNormalizationFilter(stream);
            stream = new GermanLightStemFilter(stream);
            return stream;

        }

    }

    private static CharArraySet getStopWordsForNonLuceneLangFromFile(Language language) throws WikapidiaException{

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

//            // Brent's code version:
//            Reader r = new InputStreamReader(new FileInputStream(fileName), "utf-8");
//            BufferedReader br = new BufferedReader(r);
//            CharArraySet rVal = new CharArraySet(MATCH_VERSION, 0, false);
//
//            String curLine;
//            while ((curLine = br.readLine()) != null){
//                String curSW = curLine;
//                rVal.add(curSW);
//            }
//
//            br.close();
//            return rVal;

        } catch(IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
