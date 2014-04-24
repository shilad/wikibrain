package org.wikibrain.cookbook.textgenerator;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * A program to generate text.
 *
 * @author Shilad Sen
 */
public class TextGenerator {
    Map<String, PhraseStats> stats = new HashMap<String, PhraseStats>();

    public TextGenerator() {}

    /**
     * Trains the text generator on a particular set of text.
     *
     * @param documents
     */
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
    }

    /**
     * Generates a new random text
     * @return
     */
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

    /**
     * Counts a single word.
     * @param bigram
     * @param word
     */
    private void count(String bigram, String word) {
        if (!stats.containsKey(bigram))  {
            stats.put(bigram, new PhraseStats(bigram));
        }
        stats.get(bigram).increment(word);
    }

    /**
     * Returns the PhraseStats object associated with a particular phrase.
     * @param phrase
     * @return The phrase stats object associated with the phrase, or null if it doesn't exist.
     */
    public PhraseStats getPhraseStats(String phrase) {
        return stats.get(phrase);
    }

    /**
     * Splits a text into words.
     * @param text
     * @return
     */
    private List<String> splitIntoWords(String text) {
        return Arrays.asList(text.split(" +"));
    }

    public static void main(String args[]) {
        WikiBrainWrapper wrapper = new WikiBrainWrapper(Utils.PATH_DB);
        TextGenerator generator = new TextGenerator();
        generator.train(wrapper.getPageTexts(Utils.LANG_HINDI, 2000));
        for (int i = 0; i < 3; i++) {
            System.out.println(generator.generate() + "\n\n=====================================\n");
        }
    }
}
