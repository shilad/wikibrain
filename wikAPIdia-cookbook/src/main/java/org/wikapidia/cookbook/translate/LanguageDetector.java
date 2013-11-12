package org.wikapidia.cookbook.translate;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * The language detector should detect the language of a text.
 * It must be "trained" to learn the words that appear in each language.
 * After it is trained, detect will be called for each text.
 *
 * @author Shilad Sen
 */
public class LanguageDetector {
    /**
     * Wrapper over the WikAPIdia API.
     */
    private final WikAPIdiaWrapper wrapper;

    /**
     * For each language, word -> word count
     */
    Map<Language, Map<String, Integer>> counts = new HashMap<Language, Map<String, Integer>>();

    /**
     * For each language, the number of total words in the language.
     */
    Map<Language, Integer> sums = new HashMap<Language, Integer>();

    /**
     * Constructs a new language detector.
     * @param wrapper
     */
    public LanguageDetector(WikAPIdiaWrapper wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * Learn the words used in each language.
     * Only needs to be called once each time your program is run.
     */
    public void train() {
        for (Language lang : wrapper.getLanguages()) {
            Map<String, Integer> counts = new HashMap<String, Integer>();
            int n = 0;
            for (String text : wrapper.getPageTexts(lang, 1000)) {
                for (String word : Utils.splitWords(text)) {
                    if (counts.containsKey(word)) {
                        counts.put(word, counts.get(word) + 1);
                    } else {
                        counts.put(word, 1);
                    }
                    n++;
                }
            }
            this.counts.put(lang, counts);
            sums.put(lang, n);
        }
    }

    /**
     * Detect the language associated with a text.
     * @param text
     * @return
     */
    public Language detect(String text) {
        Language bestLang = null;
        double bestScore = 0.0;
        for (Language lang : wrapper.getLanguages()) {
            double score = 0.0;
            for (String word : Utils.splitWords(text)) {
                if (counts.get(lang).containsKey(word)) {
                    score += counts.get(lang).get(word);
                }
            }
            score /= sums.get(lang);
            if (score > bestScore) {
                bestScore = score;
                bestLang = lang;
            }
        }
        return bestLang;
    }

    public static void main(String args[]) throws IOException {
        // Test to make sure the database is installed properly
        WikAPIdiaWrapper wrapper = new WikAPIdiaWrapper(Utils.PATH_DB);
        LocalPage page = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Apple");
        System.out.println("Apple in other languages:");
        for (LocalPage page2 : wrapper.getInOtherLanguages(page)) {
            System.out.println("\t" + page2.getLanguage() + ": " + page2.getTitle());
        }

        // prepare the detector
        LanguageDetector detector = new LanguageDetector(wrapper);
        detector.train();

        // use the detector
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        while (true) {
            System.out.println("Enter text to detect language, or 'stop'.");
            String text = in.readLine();
            if (text.trim().equalsIgnoreCase("stop")) {
                break;
            }
            System.out.println("language of text is " + detector.detect(text));
        }
    }
}
