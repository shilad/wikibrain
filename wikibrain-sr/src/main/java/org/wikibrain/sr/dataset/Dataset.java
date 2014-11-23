package org.wikibrain.sr.dataset;

import org.apache.commons.lang3.StringUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A gold standard dataset in some language.
 *
 * @author Ben Hillmann
 * @author Matt Lesicko
 */
public class Dataset {
    private String name;
    private Language language;
    private List<KnownSim> data;

    public Dataset(String name, Language language) {
        this.name = name;
        this.language = language;
        this.data = new ArrayList<KnownSim>();
    }

    public Dataset(String name, Language language, List<KnownSim> data) {
        this.name = name;
        this.language = language;
        this.data = data;
    }

    /**
     * Concatenates a list of datasets into a new merged dataset.
     * @param datasets
     */
    public Dataset(List<Dataset> datasets) {
        this(createJointName(datasets), datasets);
    }

    /**
     * Concatenates a list of datasets into a new merged dataset.
     * @param name
     * @param datasets
     */
    public Dataset(String name, List<Dataset> datasets) {
        if (datasets==null||datasets.isEmpty()) {
            throw new IllegalArgumentException("Attempted to create dataset from an empty list");
        }
        this.language = datasets.get(0).getLanguage();
        this.data = new ArrayList<KnownSim>();
        this.name = name;
        for (Dataset dataset : datasets) {
            if (dataset.getLanguage()!=language) {
                throw new IllegalArgumentException("Dataset language was " + language + " but attempted to add " + dataset.getLanguage());
            }
            this.data.addAll(dataset.getData());
        }
    }

    public Language getLanguage() {
        return language;
    }

    public List<KnownSim> getData() {
        return data;
    }

    public Dataset prune(double minSim, double maxSim) {
        List<KnownSim> pruned = new ArrayList<KnownSim>();
        for (KnownSim ks : data) {
            if (minSim <= ks.similarity && ks.similarity <= maxSim) {
                pruned.add(ks);
            }
        }
        return new Dataset(name + "+pruned", language, pruned);
    }

    /**
     * Shuffles a dataset and splits it into k equally sized subsets, and returns them all
     * @param k the number of desired subsets
     * @return a list of k equally sized subsets of the original dataset
     */
    public List<Dataset> split(int k) {

        if (k>data.size()){
            k=data.size();
        }
        List<KnownSim> clone = new ArrayList<KnownSim>();
        for (KnownSim ks : data){
            clone.add(ks);
        }
        Collections.shuffle(clone);
        List<Dataset> splitSets = new ArrayList<Dataset>();
        for (int i=0; i<k; i++) {
            splitSets.add(new Dataset(name + "+split-" + i, language));
        }
        for (int i=0; i< clone.size(); i++) {
            splitSets.get(i%k).getData().add(clone.get(i));
        }
        return splitSets;
    }

    public String getName() {
        return name;
    }

    private static String createJointName(List<Dataset> datasets) {
        List<String> names = new ArrayList<String>();
        for (Dataset dataset : datasets) {
            names.add(dataset.getName());
        }
        Collections.sort(names);    // makes name order insensitive
        return StringUtils.join(names, '+');
    }

    /**
     * Normalizes the range of scores to [0,1]
     */
    public void normalize() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (KnownSim ks : data) {
            if (!Double.isNaN(ks.similarity)) {
                min = Math.min(ks.similarity, min);
                max = Math.max(ks.similarity, max);
            }
        }
        if (max == min) {
            throw new IllegalStateException();
        }
        for (KnownSim ks : data) {
            ks.similarity = (ks.similarity - min) / (max-min);
        }
    }
}
