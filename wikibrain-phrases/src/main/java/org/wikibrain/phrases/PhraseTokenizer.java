package org.wikibrain.phrases;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class PhraseTokenizer {
    private final LinkProbabilityDao dao;
    private final double minLinkProbabilityForPhrases = 1E-5;

    public PhraseTokenizer(LinkProbabilityDao dao) {
        this.dao = dao;
    }

    public List<String> makePhrases(Language language, String sentence) throws DaoException {
        List<String> result = new ArrayList<String>();
        for (Token phrase : makePhraseTokens(language, sentence)) {
            result.add(phrase.getToken());
        }
        return result;
    }

    public List<String> makePhrases(Language language, List<String> words) throws DaoException {
        String text = StringUtils.join(words, " ");
        int i = 0;
        List<Token> tokens = new ArrayList<Token>();
        for (String w : words) {
            if (i > 0) {
                i++;        // for space
            }
            Token t = new Token(i, i + w.length(), text);
            if (!t.getToken().equals(w)) {
                throw new IllegalStateException();
            }
            tokens.add(t);
            i = t.getEnd();
        }
        List<String> result = new ArrayList<String>();
        for (Token phrase : makePhraseTokens(language, tokens)) {
            result.add(phrase.getToken());
        }
        return result;
    }

    public List<Token> makePhraseTokens(Language language, Token sentence) throws DaoException {
        List<Token> words = new StringTokenizer().getWordTokens(language, sentence);
        return makePhraseTokens(language, words);
    }

    public List<Token> makePhraseTokens(Language language, String sentence) throws DaoException {
        List<Token> words = new StringTokenizer().getWordTokens(language, sentence);
        return makePhraseTokens(language, words);
    }

    private static class Mention {
        TIntList tokens;
        Double probability;

        Mention(int beg, int end, double probability) {
            tokens = new TIntArrayList();
            for (int i = beg; i <= end; i++) {
                tokens.add(i);
            }
            this.probability = probability;
        }

        boolean intersects(TIntSet used) {
            for (int i : tokens.toArray()) {
                if (used.contains(i)) {
                    return true;
                }
            }
            return false;
        }
    }

    public List<Token> makePhraseTokens(Language language, List<Token> words) throws DaoException {
        if (words.isEmpty()) {
            return new ArrayList<Token>();
        }

        if (!WpCollectionUtils.isSorted(words)) {
            words = new ArrayList<Token>(words);
            Collections.sort(words);
        }

        // Pass 1: Calculate possible phrases
        List<Mention> possibles = new ArrayList<Mention>();
        for (int i = 0; i < words.size(); i++) {
            StringBuilder buffer = new StringBuilder();
            for (int j = i; j < words.size(); j++) {
                if (buffer.length() > 0) {
                    buffer.append(' ');
                }
                buffer.append(words.get(j).getToken());
                double prob = dao.getLinkProbability(buffer.toString());
                if (prob > minLinkProbabilityForPhrases) {
                    Mention m = new Mention(i, j, prob);
                    possibles.add(m);
                }
                if (!dao.isSubgram(buffer.toString(), true)) {
                    break;
                }
            }
        }

        // Pass 2: build up maximal non-overlapping set of highest-scoring mentions
        Collections.sort(possibles, new Comparator<Mention>() {
            @Override
            public int compare(Mention o1, Mention o2) {
                return -1 * o1.probability.compareTo(o2.probability);
            }
        });
        List<Mention> result = new ArrayList<Mention>();
        TIntSet used = new TIntHashSet();
        for (Mention m : possibles) {
            if (!m.intersects(used)) {
                used.addAll(m.tokens);
                result.add(m);
            }
        }

        // Pass 3: Add any words we missed
        for (int i = 0; i < words.size(); i++) {
            if (!used.contains(i)) {
                result.add(new Mention(i, i, 0.1));
            }
        }

        // Pass 4: Turn words into a sentence
        Collections.sort(result, new Comparator<Mention>() {
            @Override
            public int compare(Mention o1, Mention o2) {
                return o1.tokens.min() - o2.tokens.min();
            }
        });

        List<Token> phrases = new ArrayList<Token>();
        for (Mention m : result) {
            int begToken = m.tokens.min();
            int endToken = m.tokens.max();
            phrases.add(
                new Token(
                    words.get(begToken).getBegin(),
                    words.get(endToken).getEnd(),
                    words.get(begToken).getFullText()
                )
            );
        }
        return phrases;
    }
}
