package org.wikapidia.phrases.dao;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.wikapidia.utils.WpArrayUtils;
import org.wikapidia.utils.WpStringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * The stored record associated with a page id -> phrases and counts.
 */
public class DescriptionRecord implements Serializable, PrunableCounter {
    private int wpId;
    private String phrases[];
    private int counts[];
    private int sum = -1;        // useful after pruning.

    public DescriptionRecord(int wpId) {
        this.wpId = wpId;
    }

    public void add(String phrase, int count) {
        for (int i = 0; i < phrases.length; i++) {
            if (phrases[i].equals(phrase)) {
                counts[i] += count;
                return;
            }
        }
        counts = WpArrayUtils.grow(counts, 1);
        phrases = WpArrayUtils.grow(phrases, 1);
        counts[counts.length - 1] = count;
        phrases[phrases.length - 1] = phrase;
    }

    /**
     * Should be called after all phrases have been added.
     * Sorts the phrases by counts.
     * TODO: Keep the most popular version of a phrase instead of the normalized version.
     */
    @Override
    public void freeze() {
        final TObjectIntMap<String> normalizedCounts = new TObjectIntHashMap<String>();
        for (int i = 0; i < phrases.length; i++) {
            String norm = WpStringUtils.normalize(phrases[i]);
            normalizedCounts.adjustOrPutValue(norm, counts[i], counts[i]);
        }
        phrases = normalizedCounts.keys(new String[normalizedCounts.size()]);
        Arrays.sort(phrases, new Comparator<String>() {
            public int compare(String p1, String p2) {
                return normalizedCounts.get(p2) - normalizedCounts.get(p1);
            }
        });
        sum = 0;
        counts = new int[phrases.length];
        for (int i = 0; i < phrases.length; i++) {
            counts[i] = normalizedCounts.get(phrases[i]);
            sum += counts[i];
        }
    }

    @Override
    public void prune(int numPhrases) {
        if (numPhrases >= phrases.length) {
            return;
        }
        counts = Arrays.copyOfRange(counts, 0, numPhrases);
        phrases = Arrays.copyOfRange(phrases, 0, numPhrases);
    }

    public int getWpId() {
        return wpId;
    }

    public String[] getPhrases() {
        return phrases;
    }

    @Override
    public int[] getCounts() {
        return counts;
    }

    public String getPhrase(int i) {
        return phrases[i];
    }

    public int getCount(int i) {
        return counts[i];
    }

    public int numPhrases() {
        return phrases.length;
    }

    public int size() {
        return phrases.length;
    }

    public int getSum() {
        return sum;
    }
}
