package org.wikapidia.cookbook.entities;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * A program to extract entities from a possibly multilingual text.
 *
 * @author Shilad Sen
 */
public class EntityExtractor {
    public static final Language SIMPLE = Language.getByLangCode("simple");
    private final WikAPIdiaWrapper wrapper;
    private final LanguageDetector detector;

    /**
     * Creates a new entity extractor.
     * @param wrapper
     * @param detector
     */
    public EntityExtractor(WikAPIdiaWrapper wrapper, LanguageDetector detector) {
        this.wrapper = wrapper;
        this.detector = detector;
    }

    /**
     * Extracts entities from a text in some unknown language and prints them using System.out.
     * @param text The text to extract entities from
     * @param goal The target language to translate entities to.
     */
    public void extract(String text, Language goal) {
        Language src = detector.detect(text);
        List<String> words = Utils.splitWords(text);
        System.out.println("translating text from " + src + " to " + goal + " found entites:");
        for (int i = 0; i < words.size(); i++) {
            String[] ngrams = getNGrams(words, i, 3);
            ArrayUtils.reverse(ngrams);
            for (String phrase : ngrams) {
                if (phrase != null) {
                    if (checkEntity(phrase, src, goal)) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns an array of ngrams (unigram, bigram, etc) up to n.
     * @param words List of words
     * @param offset Initial index of ngrams
     * @param n Maximum gram length.
     * @return
     */
    private String[] getNGrams(List<String> words, int offset, int n)  {
        String ngrams[] = new String[n];
        ngrams[0] = words.get(offset);
        for (int i = 1; i < ngrams.length; i++) {
            if (offset + i < words.size()) {
                ngrams[i] = ngrams[i-1] + " " + words.get(offset+i);
            }
        }
        return ngrams;
    }

    /**
     * Describes an entity using
     * @param phrase
     * @param src
     * @param dest
     * @return
     */
    public boolean checkEntity(String phrase, Language src, Language dest) {
        if (phrase == null || phrase.length() <= 2) {
            return false;
        }
        LocalPage page = wrapper.getLocalPageByTitle(src, StringUtils.capitaliseAllWords(phrase));
        if (page == null) {
            return false;
        }
        String translated = translate(page, dest);
        if (translated == null) {
            System.out.println("\t'" + page.getTitle() + "' => uknown");
        } else {
            System.out.println("\t'" + page.getTitle() + "' => '" + translated + "'");
        }
        return true;
    }

    /**
     * Translate a local page into a destination language.
     * @param page
     * @param dest
     * @return The local page in the desired language, or null if it does not exist.
     */
    public String translate(LocalPage page, Language dest) {
        for (LocalPage page2 : wrapper.getInOtherLanguages(page)) {
            if (page2.getLanguage() == dest) {
                return page2.getTitle().getTitleStringWithoutNamespace();
            }
        }
        return null;
    }

    public static void main(String args[]) throws IOException {
        WikAPIdiaWrapper wrapper = new WikAPIdiaWrapper(Utils.PATH_DB);
        LanguageDetector detector = new LanguageDetector(wrapper);
        detector.train();
        EntityExtractor extractor = new EntityExtractor(wrapper, detector);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        while (true) {
            String text = in.readLine();
            extractor.extract(text, SIMPLE);
        }
    }
}
