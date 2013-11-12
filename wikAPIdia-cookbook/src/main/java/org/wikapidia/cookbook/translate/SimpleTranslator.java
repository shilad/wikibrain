package org.wikapidia.cookbook.translate;

import org.apache.commons.lang.StringUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Shilad Sen
 */
public class SimpleTranslator {
    public static final Language SIMPLE = Language.getByLangCode("simple");
    private final WikAPIdiaWrapper wrapper;
    private final LanguageDetector detector;

    public SimpleTranslator(WikAPIdiaWrapper wrapper, LanguageDetector detector) {
        this.wrapper = wrapper;
        this.detector = detector;
    }

    public String translate(String text, Language goal) {
        Language src = detector.detect(text);
        String translation = "";
        for (String word : splitWords(text)) {
            if (!translation.isEmpty()) {
                translation += " ";
            }
            translation += translateWord(word, src, goal);

        }
        return translation;
    }

    public String translateWord(String word, Language src, Language goal) {
        LocalPage page = wrapper.getLocalPageByTitle(src, StringUtils.capitalize(word));
        if (page == null) {
            return word;
        }
        for (LocalPage page2 : wrapper.getInOtherLanguages(page)) {
            if (page2.getLanguage() == goal) {
                return page2.getTitle().getTitleStringWithoutNamespace().toUpperCase();
            }
        }
        return word;
    }

    public String [] splitWords(String text) {
        return text.toLowerCase().split("\\s+");
    }

    public static void main(String args[]) throws IOException {
        WikAPIdiaWrapper wrapper = new WikAPIdiaWrapper("../db");
        LanguageDetector detector = new LanguageDetector(wrapper);
        detector.train();
        SimpleTranslator translator = new SimpleTranslator(wrapper, detector);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
        while (true) {
            String text = in.readLine();
            System.out.println("text is " + translator.translate(text, SIMPLE));
        }
    }
}
