package org.wikapidia.cookbook.prosemaker;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author Shilad Sen
 */
public class TextGenerator {
    Map<String, PhraseStats> stats = new HashMap<String, PhraseStats>();

    public TextGenerator() {}

    public void train(List<String> documents) {
        for (String document : documents) {
            List<String> words = splitIntoWords(document);
            for (int i = 0; i <= words.size(); i++) {
                String from;
                if (i == 0) {
                    from = "";
                } else if (i == 1) {
                    from = words.get(0);
                } else {
                    from = words.get(i-2) + " " + words.get(i-1);
                }
                String to = (i == words.size()) ? "" : words.get(i);
                count(from, to);
            }
        }
        System.out.println("trained on " + stats.size() + " bigrams");
    }

    public String generate() {
        List<String> words = new ArrayList<String>();
        while (true) {
            int n = words.size();
            String from;
            if (n == 0) {
                from = "";
            } else if (n == 1) {
                from = words.get(0);
            } else {
                from = words.get(n-2) + " " + words.get(n-1);
            }
            if (!stats.containsKey(from)) {
                break;
            }
            words.add(stats.get(from).pickRandomTo());
        }
        return StringUtils.join(words, " ") + ". ";
    }

    private void count(String bigram, String word) {
        if (!stats.containsKey(bigram))  {
            stats.put(bigram, new PhraseStats(bigram));
        }
        stats.get(bigram).increment(word);
    }

    private List<String> splitIntoWords(String sentence) {
        return Arrays.asList(sentence.split(" +"));
    }

    public static void main(String args[]) {
        WikAPIdiaWrapper wrapper = new WikAPIdiaWrapper(Utils.PATH_DB);
        TextGenerator generator = new TextGenerator();
        generator.train(wrapper.getPageTexts(Utils.LANG_SIMPLE, 2000));
        for (int i = 0; i < 3; i++) {
            System.out.println(generator.generate() + "\n\n=====================================\n");
        }
    }
}
