package org.wikibrain.sr.utils;

import org.wikibrain.core.lang.Language;

/**
 * A single human labeled entry from the gold standard dataset.
 *
 * If the Wikipedia ids wpid1 and wpid2 are not -1, phrase1 and phrase2 contain
 * Wikipedia article titles (not random phrases), and the titles have been mapped
 * to Wikipedia ids.
 */
public class KnownSim implements Comparable<KnownSim> {
    public String phrase1;
    public String phrase2;
    public int wpId1 = -1;
    public int wpId2 = -1;
    public Language language;
    public double similarity;

    public KnownSim(String phrase1, String phrase2, double similarity, Language language) {
        this.phrase1 = phrase1;
        this.phrase2 = phrase2;
        this.similarity = similarity;
        this.language = language;
    }

    public KnownSim(String phrase1, String phrase2, int wpId1, int wpId2, double similarity, Language language) {
        this.wpId1 = wpId1;
        this.wpId2 = wpId2;
        this.phrase1 = phrase1;
        this.phrase2 = phrase2;
        this.similarity = similarity;
        this.language = language;
    }

    @Override
    public String toString() {
        return "KnownSim{" +
                "phrase1='" + phrase1 + '\'' +
                ", phrase2='" + phrase2 + '\'' +
                ", similarity=" + similarity +
                '}';
    }

    /**
     * Swaps phrase1 and phrase2 50% of the time
     */
    public void maybeSwap() {
        if (Math.random() > 0.5) {
            String tp = phrase1;
            phrase1 = phrase2;
            phrase2 = tp;
            int tid = wpId1;
            wpId1 = wpId2;
            wpId2 = tid;
        }
    }

    public KnownSim getReversed() {
        return new KnownSim(phrase2, phrase1, wpId2, wpId1, similarity, language);
    }

    /**
     * Sort by low to high similarity
     * @param knownSim
     * @return
     */
    @Override
    public int compareTo(KnownSim knownSim) {
        if (similarity < knownSim.similarity) {
            return -1;
        } else if (similarity > knownSim.similarity) {
            return 1;
        } else if (phrase1.compareTo(knownSim.phrase1) != 0){
            return phrase1.compareTo(knownSim.phrase1);
        } else {
            return phrase2.compareTo(knownSim.phrase2);
        }
    }
}
