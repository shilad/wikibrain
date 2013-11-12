package org.wikapidia.cookbook.translate;

import org.apache.commons.lang.StringUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class EntityExtractor {
    public static final Language SIMPLE = Language.getByLangCode("simple");
    private final WikAPIdiaWrapper wrapper;
    private final LanguageDetector detector;

    public EntityExtractor(WikAPIdiaWrapper wrapper, LanguageDetector detector) {
        this.wrapper = wrapper;
        this.detector = detector;
    }

    public void extract(String text, Language goal) {
        Language src = detector.detect(text);
        List<String> words = Utils.splitWords(text);
        System.out.println("translating text from " + src + " to " + goal + " found entites:");
        for (int i = 0; i < words.size(); i++) {
            if (i < words.size()-2) {
                String trigram = words.get(i) + " " + words.get(i+1) + " " + words.get(i+2);
                if (isEntity(trigram, src, goal)) {
                    i += 2;
                    continue;
                }
            }
            if (i < words.size()-1) {
                String bigram = words.get(i) + " " + words.get(i+1);
                if (isEntity(bigram, src, goal)) {
                    i += 1;
                    continue;
                }
            }
            isEntity(words.get(i), src, goal);
        }
    }

    public boolean isEntity(String phrase, Language src, Language dest) {
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
