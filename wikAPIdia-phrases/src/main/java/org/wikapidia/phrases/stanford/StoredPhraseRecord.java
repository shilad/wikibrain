package org.wikapidia.phrases.stanford;

import org.wikapidia.utils.WpArrayUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The record associated with stored phrase -> pages and counts.
 */
public class StoredPhraseRecord implements Serializable {
    private String text;    // normalized version of the text
    private int wpIds[] = new int[0];
    private int counts[] = new int[0];

    public StoredPhraseRecord(String text) {
        this.text = text;
    }

    public void add(int wpId, int count) {
        for (int i = 0; i < wpIds.length; i++) {
            if (wpIds[i] == wpId) {
                counts[i] += count;
                return;
            }
        }

        counts = WpArrayUtils.grow(counts, 1);
        wpIds = WpArrayUtils.grow(wpIds, 1);
        counts[counts.length - 1] = count;
        wpIds[wpIds.length - 1] = wpId;
    }

    /**
     * Should be called after all wpids have been added.
     * Sorts the ids by counts.
     */
    public void freeze() {
        Integer [] indexes = new Integer[counts.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            public int compare(Integer i, Integer j) {
                return counts[j] - counts[i];
            }
        });
        int newWpIds [] = new int[wpIds.length];
        int newCounts[] = new int[counts.length];
        for (int i = 0; i < newCounts.length; i++) {
            int j = indexes[i];
            newWpIds[i] = wpIds[j];
            newCounts[i] = counts[j];
        }
    }

    public String getText() {
        return text;
    }

    public int[] getWpIds() {
        return wpIds;
    }

    public int[] getCounts() {
        return counts;
    }
}
