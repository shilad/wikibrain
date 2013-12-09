package org.wikapidia.sr.evaluation;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.utils.KnownSim;

import java.util.*;

/**
 * Groups similarities for a particular phrases together to form ranked most similar lists.
 *
 * @author Shilad Sen
 */
public class MostSimilarDataset {
    private static final double DEFAULT_THRESHOLD = 0.0;

    private final Language language;
    private final Map<String, KnownMostSim> data;

    /**
     * Creates a new most similar dataset based on some input datasets.
     * KnownSims with similarity less than DEFAULT_THRESHOLD are ignored.
     *
     * @param datasets
     */
    public MostSimilarDataset(List<Dataset> datasets) {
        this(datasets, DEFAULT_THRESHOLD);
    }

    /**
     * Creates a new most similar dataset based on some input datasets.
     * KnownSims with similarity less than threshold are ignored.
     *
     * @param datasets
     */
    public MostSimilarDataset(List<Dataset> datasets, double threshold) {
        if (datasets.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.language = datasets.get(0).getLanguage();
        Map<String, List<KnownSim>> sims = new HashMap<String, List<KnownSim>>();
        for (Dataset ds : datasets) {
            ds.normalize(); // just to be safe
            if (ds.getLanguage() != language) {
                throw new IllegalArgumentException("All datasets must be the same language");
            }
            for (KnownSim ks : ds.getData()) {
                addToMap(sims, ks);
                addToMap(sims, ks.getReversed());
            }
        }
        data = new HashMap<String, KnownMostSim>();
        for (String phrase : sims.keySet()) {
            KnownMostSim mostSim = new KnownMostSim(sims.get(phrase), threshold);
            if (mostSim.getMostSimilar().size() > 0) {
                data.put(phrase, mostSim);
            }
        }
    }

    public Set<String> getPhrases() {
        return data.keySet();
    }

    public KnownMostSim getSimilarities(String phrase) {
        return data.get(phrase);
    }

    private void addToMap(Map<String, List<KnownSim>> sims, KnownSim ks) {
        if (!sims.containsKey(ks.phrase1)) {
            sims.put(ks.phrase1, new ArrayList<KnownSim>());
        }
        sims.get(ks.phrase1).add(ks);
    }
}
