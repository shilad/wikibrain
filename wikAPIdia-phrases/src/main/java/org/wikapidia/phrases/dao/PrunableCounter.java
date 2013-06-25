package org.wikapidia.phrases.dao;

import java.io.Serializable;

/**
 * Keeps track of counts for something.
 */
public interface PrunableCounter extends Serializable {
    /**
     * Must be called when the counts are finished being tallied, before pruning.
     */
    void freeze();

    /**
     * Prunes the entries back to the top-k by count.
     * @param numEntries
     */
    void prune(int numEntries);

    /**
     * Returns the counts for each entry.
     * If freeze was previously called, they will be in descending order.
     * @return
     */
    int[] getCounts();

    /**
     * @return The sum of all elements - before pruning.
     */
    int getSum();
}
