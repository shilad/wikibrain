package org.wikibrain.core.nlp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Adapted from http://stackoverflow.com/questions/3656762/n-gram-generation-from-a-sentence
 * @author Shilad Sen
 */
public class NGramCreator {

    public List<String> getNGrams(List<String> words, int n) {
        return getNGrams(words, n, n);
    }

    public List<String> getNGrams(List<String> words, int minN, int maxN) {
        List<String> ngrams = new ArrayList<String>();
        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i < words.size() - n + 1; i++) {
                ngrams.add(concat(words, i, i+n));
            }
        }
        return ngrams;
    }

    public List<Token> getNGramTokens(List<Token> words, int minN, int maxN) {
        List<Token> ngrams = new ArrayList<Token>();
        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i < words.size() - n + 1; i++) {
                ngrams.add(new Token(words.subList(i, i+n)));
            }
        }
        return ngrams;
    }

    public static String concat(List<String> words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++)
            sb.append((i > start ? " " : "") + words.get(i));
        return sb.toString();
    }
}
