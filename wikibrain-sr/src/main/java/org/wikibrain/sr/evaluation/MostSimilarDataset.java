package org.wikibrain.sr.evaluation;

import org.apache.commons.lang3.StringUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.utils.KnownSim;

import java.util.*;

/**
 * Groups similarities for a particular phrases together to form ranked most similar lists.
 *
 * @author Shilad Sen
 */
public class MostSimilarDataset {
    private static final double DEFAULT_THRESHOLD = Double.NEGATIVE_INFINITY;

    private final String name;
    private final Language language;
    private final Map<String, KnownMostSim> data;

    private MostSimilarDataset(Language language, String name) {
        this.language = language;
        this.name = name;
        this.data = new HashMap<String, KnownMostSim>();
    }

    /**
     * @see #MostSimilarDataset(java.util.List)
     * @param dataset
     */
    public MostSimilarDataset(Dataset dataset) {
        this(Arrays.asList(dataset));
    }

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
        List<String> names = new ArrayList<String>();
        for (Dataset ds : datasets) {
            ds.normalize(); // just to be safe
            if (ds.getLanguage() != language) {
                throw new IllegalArgumentException("All datasets must be the same language");
            }
            for (KnownSim ks : ds.getData()) {
                addToMap(sims, ks);
                addToMap(sims, ks.getReversed());
            }
            names.add(ds.getName());
        }
        name = StringUtils.join(names, ",") +
                ((threshold == DEFAULT_THRESHOLD) ? "" : ("+threshold="+threshold));
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

    /**
     * Returns a new dataset that only contains phrases with at least n KnownSim entries.
     * @param n Minimum number of phrases
     * @return
     */
    public MostSimilarDataset pruneSmallLists(int n) {
        MostSimilarDataset pruned = new MostSimilarDataset(language, name + "+pruned=" + n);
        for (String phrase : data.keySet()) {
            if (data.get(phrase).getMostSimilar().size() >= n) {
                pruned.data.put(phrase, data.get(phrase));
            }
        }
        return pruned;
    }

    private void addToMap(Map<String, List<KnownSim>> sims, KnownSim ks) {
        if (!sims.containsKey(ks.phrase1)) {
            sims.put(ks.phrase1, new ArrayList<KnownSim>());
        }
        sims.get(ks.phrase1).add(ks);
    }

    public String getName() {
        return name;
    }

    public Language getLanguage() {
        return language;
    }

    /**
     * Converts the most similar dataset back to a "normal" dataset.
     * @return
     */
    public Dataset toDataset() {
        List<KnownSim> sims = new ArrayList<KnownSim>();
        for (KnownMostSim kms : data.values()) {
            sims.addAll(kms.getMostSimilar());
        }
        return new Dataset(name, language, sims);
    }

    /**
     * Returns a list of suitable test cross-validation sets.
     * The splits occur along phrases, so all entries for a particular phrase stay in the
     * same cross-validation split.
     * @param n
     * @return
     */
    public List<MostSimilarDataset> split(int n) {
        List<String> phrases = new ArrayList<String>(data.keySet());
        Collections.shuffle(phrases);
        List<MostSimilarDataset> result = new ArrayList<MostSimilarDataset>();
        for (int i = 0; i < n; i++) {
            result.add(new MostSimilarDataset(language, name + "+split-" + i));
        }
        for (int i = 0; i < phrases.size(); i++) {
            String p = phrases.get(i);
            result.get(i % n).data.put(p, data.get(p));
        }
        return result;
    }

    /**
     * @see #split(int)
     * @see #toDataset()
     * @param n
     * @return
     */
    public List<Dataset> splitIntoDatasets(int n) {
        List<Dataset> result = new ArrayList<Dataset>();
        for (MostSimilarDataset msd : split(n)) {
            result.add(msd.toDataset());
        }
        return result;
    }
}
