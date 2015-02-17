package org.wikibrain.sr.evaluation;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class KnownMostSim {
    private final Language language;
    private final String phrase;
    private final int pageId;
    private final List<KnownSim> mostSimilar;

    /**
     * Creates a new KnownMostSim without a similarity threshold (i.e. keeps everything).
     * @see #KnownMostSim(java.util.List, double)
     * @param mostSim
     */
    public KnownMostSim(List<KnownSim> mostSim) {
        this(mostSim, Double.NEGATIVE_INFINITY);
    }

    /**
     * Creates a new KnownMostSim from a list of KnownSims.
     *
     * Each KnownSim's phrase1 and language must be identical.
     * If the list has duplicate phrase2, they will be merged into a single KnownSim with the mean similarity score.
     * All (postmerged) KnownSims with similarity less than threshold will be removed.
     * The final list is sorted in reverse order of similarity.
     *
     * @param mostSim
     */
    public KnownMostSim(List<KnownSim> mostSim, double threshold) {
        if (mostSim.isEmpty()) {
            throw new IllegalArgumentException();
        }

        // set and check the phrase and language
        phrase = mostSim.get(0).phrase1;
        language = mostSim.get(0).language;
        for (KnownSim ks : mostSim) {
            if (!ks.phrase1.equals(phrase)) {
                throw new IllegalArgumentException("expected phrase " + phrase + ", received " + ks.phrase1);
            }
            if (!ks.language.equals(language)) {
                throw new IllegalArgumentException("expected phrase " + language + ", received " + ks.language);
            }
        }

        // set the most common local page id
        int maxIdCount = 0;
        int maxId = -1;
        TIntIntMap idCounts = new TIntIntHashMap();
        for (KnownSim ks : mostSim) {
            if (ks.wpId1 >= 0) {
                int n = idCounts.adjustOrPutValue(ks.wpId1, 1, 1);
                if (n > maxIdCount) {
                    maxIdCount = n;
                    maxId = ks.wpId1;
                }
            }
        }
        this.pageId = maxId;

        // Set the mean scores for other phrases
        TObjectIntMap<String> ids = new TObjectIntHashMap<String>();
        TObjectIntMap<String> counts = new TObjectIntHashMap<String>();
        TObjectDoubleMap<String> sums = new TObjectDoubleHashMap<String>();
        this.mostSimilar = new ArrayList<KnownSim>();
        for (KnownSim ks : mostSim) {
            ids.put(ks.phrase2, ks.wpId2);
            counts.adjustOrPutValue(ks.phrase2, 1, 1);
            sums.adjustOrPutValue(ks.phrase2, ks.similarity, ks.similarity);
        }
        for (String phrase2 : counts.keySet()) {
            double mean = sums.get(phrase2) / counts.get(phrase2);
            if (mean >= threshold) {
                mostSimilar.add(new KnownSim(phrase, phrase2, pageId, ids.get(phrase2), mean, language));
            }
        }
        Collections.sort(this.mostSimilar);
        Collections.reverse(this.mostSimilar);
    }

    public KnownMostSim getAboveThreshold(double threshold) {
        return new KnownMostSim(mostSimilar, threshold);
    }

    public Language getLanguage() {
        return language;
    }

    public List<KnownSim> getMostSimilar() {
        return mostSimilar;
    }

    public String getPhrase() {
        return phrase;
    }

    public int getPageId() {
        return pageId;
    }
}
