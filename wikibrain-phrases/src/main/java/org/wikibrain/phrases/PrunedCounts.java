package org.wikibrain.phrases;

import org.wikibrain.utils.WpStringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * An ordered map by
 * Captures the top-k entries, by count, but also remembers the total count.
 * @param <K> The class of the objects being counted.
 *
 * @author Shilad Sen
 */
public class PrunedCounts<K> extends LinkedHashMap<K, Integer> implements Serializable {
    private int total;

    public PrunedCounts(int total) {
        this.total = total;
    }

    /**
     * @return The total count of the data (before pruning).
     */
    public int getTotal() {
        return total;
    }

    /**
     * Prunes counts down.
     * Returns the pruned counts (i.e. with some keys removed, sorted by count, and total unchanged)
     * or null if the entry should not appear in the database at all. The resulting hashmap
     * is in decreasing order by size.
     */
    public static interface Pruner<K> {
        public PrunedCounts<K> prune(final Map<K, Integer> allCounts);

    }

    @Override
    public String toString() {
        return "total="+total + ", " + super.toString();
    }
}
