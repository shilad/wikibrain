package org.wikibrain.phrases;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;

import java.util.*;

/**
 * A simple pruner for PrunedCounts.
 * Prunes by count, rank, and fraction of total count.
 *
 * @author Shilad Sen
 */
public class SimplePruner<K> implements PrunedCounts.Pruner<K> {
    private final int minCount;
    private final int maxRank;
    private final double minFrac;

    public SimplePruner(int minCount, int maxRank, double minFrac) {
        this.minCount = minCount;
        this.maxRank = maxRank;
        this.minFrac = minFrac;
    }

    /**
     * Prunes counts down.
     * Returns the pruned counts (i.e. with some keys removed, sorted by count, and total unchanged)
     * or null if the entry should not appear in the database at all. The resulting hashmap
     * is in decreasing order by size.
     * @param allCounts All counts.
     * @return
     */
    @Override
    public PrunedCounts<K> prune(final Map<K, Integer> allCounts) {
        List<K> keys = new ArrayList<K>(allCounts.keySet());
        Collections.sort(keys, new Comparator<K>() {
            @Override
            public int compare(K key1, K key2) {
                return -1 * (allCounts.get(key1) - allCounts.get(key2));
            }
        });
        int sum = 0;
        for (Integer c : allCounts.values()) { sum += c; }
        PrunedCounts<K> pruned = new PrunedCounts<K>(sum);
        for (K key : keys) {
            int c = allCounts.get(key);
            if (pruned.size() >= maxRank || c < minCount || 1.0 * c / sum < minFrac)
                break;
            pruned.put(key, c);
        }
        if (pruned.isEmpty()) {
            return null;
        } else {
            return pruned;
        }
    }

    public static class Provider extends org.wikibrain.conf.Provider<PrunedCounts.Pruner> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PrunedCounts.Pruner.class;
        }

        @Override
        public String getPath() {
            return "phrases.pruners";
        }

        @Override
        public PrunedCounts.Pruner get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("simple")) {
                return null;
            }
            int minCount = config.getInt("minCount");
            int maxRank = config.getInt("maxRank");
            double minFraction = config.getDouble("minFraction");
            return new SimplePruner(minCount, maxRank, minFraction);
        }
    }
}
