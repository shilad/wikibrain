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
    private static final double DEFAULT_THRESHOLD = 0.4;

    private final Language language;
    private final Map<String, List<KnownSim>> data;

    /**
     * Creates a new most similar dataset based on some input datasets.
     * KnownSims with similarity less than DEFAULT_THRESHOLD are ignored.
     *
     * @param datasets
     */
    public MostSimilarDataset(Dataset ... datasets) {
        this(Arrays.asList(datasets));
    }

    /**
     * Creates a new most similar dataset based on some input datasets.
     * KnownSims with similarity less than threshold are ignored.
     *
     * @param datasets
     */
    public MostSimilarDataset(double threshold, Dataset ... datasets) {
        this(Arrays.asList(datasets), threshold);
    }

    /**
     * Creates a new most similar dataset based on some input datasets.
     * KnownSims with similarity less than DEFAULT_THRESHOLD are ignored.
     *
     * @param datasets
     */
    public MostSimilarDataset(List<Dataset> datasets) {
        this(datasets, 0.5);
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
                if (ks.similarity >= threshold) {
                    addToMap(sims, ks);
                    addToMap(sims, ks.getReversed());
                }
            }
        }
        data = new HashMap<String, List<KnownSim>>();
        for (String phrase1 : sims.keySet()) {
            TObjectIntMap<String> counts = new TObjectIntHashMap<String>();
            TObjectDoubleMap<String> sums = new TObjectDoubleHashMap<String>();
            for (KnownSim ks : sims.get(phrase1)) {
                counts.adjustOrPutValue(ks.phrase2, 1, 1);
                sums.adjustOrPutValue(ks.phrase2, ks.similarity, ks.similarity);
            }
            List<KnownSim> phraseSims = new ArrayList<KnownSim>();
            for (String phrase2 : counts.keySet()) {
                double mean = sums.get(phrase2) / counts.get(phrase2);
                phraseSims.add(new KnownSim(phrase1, phrase2, mean, language));
            }
            Collections.sort(phraseSims);
            Collections.reverse(phraseSims);
            data.put(phrase1, phraseSims);
        }
    }

    public Set<String> getPhrases() {
        return data.keySet();
    }

    public List<KnownSim> getSimilarities(String phrase) {
        return data.get(phrase);
    }

    private void addToMap(Map<String, List<KnownSim>> sims, KnownSim ks) {
        if (!sims.containsKey(ks.phrase1)) {
            sims.put(ks.phrase1, new ArrayList<KnownSim>());
        }
        sims.get(ks.phrase1).add(ks);
    }
}
