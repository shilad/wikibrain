package org.wikibrain.cookbook.overlap;

import org.wikibrain.core.model.LocalPage;

/**
 * @author Shilad Sen
 */
public class LocalPagePopularity implements Comparable<LocalPagePopularity> {
    private LocalPage page;
    private int numInLinks;

    public LocalPagePopularity(LocalPage page, int numInLinks) {
        this.page = page;
        this.numInLinks = numInLinks;
    }

    public LocalPage getPage() {
        return page;
    }

    public int getPopularity() {
        return numInLinks;
    }

    @Override
    public int compareTo(LocalPagePopularity localPagePopularity) {
        return numInLinks - localPagePopularity.numInLinks;
    }
}
