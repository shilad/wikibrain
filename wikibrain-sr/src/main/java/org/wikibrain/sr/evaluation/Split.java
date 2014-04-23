package org.wikibrain.sr.evaluation;

import org.wikibrain.sr.dataset.Dataset;

/**
 * A single train / test split.
 *
 * @author Shilad Sen
 */
public class Split {
    private String name;
    private String group;
    private Dataset train;
    private Dataset test;

    public Split(String name, String group, Dataset train, Dataset test) {
        this.name = name;
        this.group = group;
        this.train = train;
        this.test = test;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public Dataset getTrain() {
        return train;
    }

    public Dataset getTest() {
        return test;
    }
}
