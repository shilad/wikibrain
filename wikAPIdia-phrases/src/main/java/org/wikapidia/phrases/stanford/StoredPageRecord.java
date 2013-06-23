package org.wikapidia.phrases.stanford;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.wikapidia.utils.WpArrayUtils;
import org.wikapidia.utils.WpStringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * The stored record associated with a page id -> phrases and counts.
 */
public class StoredPageRecord implements Serializable {
    private int wpId;
    private String phrases[];
    private int counts[];

    public StoredPageRecord(int wpId) {
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
        counts = new int[phrases.length];
        for (int i = 0; i < phrases.length; i++) {
            counts[i] = normalizedCounts.get(phrases[i]);
        }
    }

    public int getWpId() {
        return wpId;
    }

    public String[] getPhrases() {
        return phrases;
    }

    public int[] getCounts() {
        return counts;
    }
}
