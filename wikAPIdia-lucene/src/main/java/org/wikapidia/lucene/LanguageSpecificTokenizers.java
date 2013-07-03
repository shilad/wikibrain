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
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.tartarus.snowball.ext.*;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static org.wikapidia.lucene.LuceneUtils.*;

/**
 *
 * @author Ari Weiland
 *
 * This class is based on a class of the same name from Brent Hecht, WikAPIdia.
 * I have updated everything to properly function consistently with lucene 4.3,
 * as well as adding functionality such as the booleans to determine which filters
 * should be applied.
 *
 */
public class LanguageSpecificTokenizers {

    private static final String STOP_WORDS = "src/main/resources/stopwords/";

//    // Just a test to make sure tokenizers are working
//    public static void main(String[] args){
//        try{
//            Field textField = new TextField("test", "wrap around the world", Field.Store.YES);
//            List<String> langCodes = conf.get().getStringList("languages");
//            langCodes.add("he");
//            langCodes.add("sk");
//            LanguageSet langSet = new LanguageSet(langCodes);
//            for(Language language : langSet){
//                WikapidiaAnalyzer wa = new WikapidiaAnalyzer(language);
//                IndexWriterConfig iwc = new IndexWriterConfig(MATCH_VERSION, wa);
//                iwc.setRAMBufferSizeMB(1024.0);
//                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
//                IndexWriter writer = new IndexWriter(new RAMDirectory(), iwc);
//                Document d = new Document();
//                d.add(textField);
//                writer.addDocument(d);
//                writer.close();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }


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
        } catch (Exception e) {
            throw new WikapidiaException(e);
        }
    }

    public static abstract class WLanguageTokenizer {

        protected boolean caseInsensitive = true;
        protected boolean useStopWords = true;
        protected boolean useStem = true;

        protected abstract TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException;

        public Tokenizer getTokenizer(Reader r) {
            return new StandardTokenizer(MATCH_VERSION, r);
        }

        public void setFilters(FilterSelect select) {
            this.caseInsensitive = select.isCaseInsensitive();
            this.useStopWords = select.doesUseStopWords();
            this.useStem = select.doesUseStem();
        }

        public FilterSelect getFilters() {
            FilterSelect filterSelect = new FilterSelect();
            if (caseInsensitive) filterSelect.caseInsensitive();
            if (useStopWords) filterSelect.useStopWords();
            if (useStem) filterSelect.useStem();
            return filterSelect;
        }
    }

    public static class SpanishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive) 
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, SpanishAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SpanishLightStemFilter(stream);
            }
            return stream;
        }
    }

    public static class HungarianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, HungarianAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new HungarianStemmer());
            }
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

    public static class IndonesianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, IndonesianAnalyzer.getDefaultStopSet());
            if (useStem && !stemExclusionSet.isEmpty()) {
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
            if (useStopWords) {
                stream = new JapanesePartOfSpeechStopFilter(true, stream, JapaneseAnalyzer.getDefaultStopTags());
                stream = new StopFilter(MATCH_VERSION, stream, JapaneseAnalyzer.getDefaultStopSet());
            }
            if (useStem)
                stream = new JapaneseKatakanaStemFilter(stream);
            return stream;
        }
    }

    public static class KoreanTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new CJKWidthFilter(input);
            stream = new CJKBigramFilter(stream); // TODO: This seems to be a default, but we should look into it
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
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
            if (useStopWords) {
                stream = new ElisionFilter(stream, DEFAULT_ARTICLES);
                stream = new StopFilter(MATCH_VERSION, stream, ItalianAnalyzer.getDefaultStopSet());
            }
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new ItalianLightStemFilter(stream);
            }
            return stream;
        }
    }

    public static class DutchTokenizer extends WLanguageTokenizer {

//        static final CharArrayMap<String> DEFAULT_STEM_DICT;
//        static {
//            DEFAULT_STEM_DICT = new CharArrayMap<String>(MATCH_VERSION, 4, false);
//            DEFAULT_STEM_DICT.put("fiets", "fiets"); //otherwise fiet
//            DEFAULT_STEM_DICT.put("bromfiets", "bromfiets"); //otherwise bromfiet
//            DEFAULT_STEM_DICT.put("ei", "eier");
//            DEFAULT_STEM_DICT.put("kind", "kinder");
//        }

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
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

    public static class NorwegianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, NorwegianAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new NorwegianStemmer());
            }
            return stream;
        }
    }


    /**
     * Previous implementation was identical to spanish, so I simplified...
     */
    public static class LadinoTokenizer extends SpanishTokenizer {}

    public static class PortugueseTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, PortugueseAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new PortugueseLightStemFilter(stream);
            }
            return stream;
        }
    }

    public static class RomanianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet)
                throws WikapidiaException {

            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, RomanianAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new RomanianStemmer());
            }
            return stream;
        }
    }

    public static class RussianTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, RussianAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new RussianStemmer());
            }
            return stream;
        }
    }

    /**
     * Just using Russian for Ukrainian for now
     * @author bjhecht
     */
    public static class UkrainianTokenizer extends RussianTokenizer{};

    public static class SwedishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, RomanianAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new SwedishStemmer());
            }
            return stream;
        }
    }

    public static class TurkishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, RomanianAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new TurkishStemmer());
            }
            return stream;
        }
    }

    public static class ChineseTokenizer extends WLanguageTokenizer{

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new WordTokenFilter(input); // breaks Sentences into words
            // stream = new LowerCaseFilter(stream);
            // LowerCaseFilter is not needed, as SegTokenFilter lowercases Basic Latin text.
            // The porter stemming is too strict, this is not a bug, this is a feature:)
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, SmartChineseAnalyzer.getDefaultStopSet());
            if (useStem)
                stream = new PorterStemFilter(stream);
            return stream;
        }


    }

    public static class HebrewTokenizer extends WLanguageTokenizer {

        private static CharArraySet stopWords = null;

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            if (stopWords == null){
                stopWords = getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("he"));
            }
            TokenStream stream = input;
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, input, stopWords);
            return stream;
        }
    }

    public static class SlovakTokenizer extends WLanguageTokenizer {

        private static CharArraySet stopWords = null;

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            if (stopWords == null){
                stopWords = getStopWordsForNonLuceneLangFromFile(Language.getByLangCode("sk"));
            }
            TokenStream stream = input;
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, input, stopWords);
            return stream;
        }
    }

    public static class EnglishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException{
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, EnglishAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new EnglishPossessiveFilter(MATCH_VERSION, stream);
                stream = new PorterStemFilter(stream);
            }
            return stream;
        }
    }

    public static class DanishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, DanishAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new DanishStemmer());
            }
            return stream;
        }
    }

    public static class FrenchTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords) {
                stream = new ElisionFilter(stream, FrenchAnalyzer.DEFAULT_ARTICLES);
                stream = new StopFilter(MATCH_VERSION, stream, FrenchAnalyzer.getDefaultStopSet());
            }
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new FrenchLightStemFilter(stream);
            }
            return stream;
        }
    }

    public static class FinnishTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, FinnishAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new FinnishStemmer());
            }
            return stream;
        }
    }

    public static class CzechTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, CzechAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new CzechStemFilter(stream);
            }
            return stream;
        }
    }

    public static class CatalanTokenizer extends WLanguageTokenizer {

        private static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
                new CharArraySet(MATCH_VERSION, Arrays.asList("d", "l", "m", "n", "s", "t"), true));

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords) {
                stream = new ElisionFilter(stream, DEFAULT_ARTICLES);
                stream = new StopFilter(MATCH_VERSION, stream, DanishAnalyzer.getDefaultStopSet());
            }
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new SnowballFilter(stream, new CatalanStemmer());
            }
            return stream;
        }
    }

    public static class GermanTokenizer extends WLanguageTokenizer {

        @Override
        protected TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) throws WikapidiaException {
            TokenStream stream = new StandardFilter(MATCH_VERSION, input);
            stream = new GermanNormalizationFilter(stream);
            if (caseInsensitive)
                stream = new LowerCaseFilter(MATCH_VERSION, stream);
            if (useStopWords)
                stream = new StopFilter(MATCH_VERSION, stream, GermanAnalyzer.getDefaultStopSet());
            if (useStem) {
                if (!stemExclusionSet.isEmpty())
                    stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
                stream = new GermanLightStemFilter(stream);
            }
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
        } catch (IOException e) {
            throw new WikapidiaException(e);
        }
    }
}
