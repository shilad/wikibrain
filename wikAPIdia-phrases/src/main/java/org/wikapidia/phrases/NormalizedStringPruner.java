package org.wikapidia.phrases;

import org.wikapidia.utils.WpStringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends the simple pruner by treating two strings that have the
 * same "normalized" version as the same.
 *
 * The final pruned counts includes the most popular version of the
 * normalized string.
 *
 * @author Shilad Sen
 */
public class NormalizedStringPruner extends SimplePruner<String> {
    public NormalizedStringPruner(int minCount, int maxRank, double minFrac) {
        super(minCount, maxRank, minFrac);
    }

    @Override
    public PrunedCounts<String> prune(final Map<String, Integer> allCounts) {
        Map<String, Integer> sums = new HashMap<String, Integer>(); // count sums per normalized string
        Map<String, String> best = new HashMap<String, String>();   // normalized string to most popular unnormalized version
        for (String key : allCounts.keySet()) {
            String nkey = WpStringUtils.normalize(key);
            int c = allCounts.get(key);
            sums.put(nkey, c + (sums.containsKey(nkey) ? sums.get(nkey) : 0));
            if (!best.containsKey(nkey) || allCounts.get(best.get(nkey)) < c) {
                best.put(nkey, key);
            }
        }
        Map<String, Integer> normalizedCounts = new HashMap<String, Integer>();
        for (String key : best.values()) {
            normalizedCounts.put(key, sums.get(WpStringUtils.normalize(key)));
        }
        return super.prune(normalizedCounts);
    }
}
