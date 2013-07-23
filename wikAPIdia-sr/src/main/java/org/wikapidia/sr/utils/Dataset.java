package org.wikapidia.sr.utils;

import org.wikapidia.core.lang.Language;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ben Hillmann
 * @author Matt Lesicko
 */
public class Dataset {
    public Language language;
    public List<KnownSim> data;

    public Dataset(Language language) {
        this.language = language;
        this.data = new ArrayList<KnownSim>();
    }

    public Dataset(Language language, List<KnownSim> data) {
        this.language = language;
        this.data = data;
    }

    public Dataset(List<Dataset> datasets) {
        if (datasets==null||datasets.isEmpty()) {
            throw new IllegalArgumentException("Attempted to create dataset from an empty list");
        }
        this.language = datasets.get(0).getLanguage();
        this.data = new ArrayList<KnownSim>();
        for (Dataset dataset:datasets) {
            if (dataset.getLanguage()!=language) {
                throw new IllegalArgumentException("Dataset language was " + language + " but attempted to add " + dataset.getLanguage());
            }
            this.data.addAll(dataset.getData());

        }
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public List<KnownSim> getData() {
        return data;
    }

    public void setData(List<KnownSim> data) {
        this.data = data;
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
            splitSets.add(new Dataset(language));
        }
        for (int i=0; i< clone.size(); i++) {
            splitSets.get(i%k).getData().add(clone.get(i));
        }
        return splitSets;
    }
}
