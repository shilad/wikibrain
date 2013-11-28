package org.wikapidia.cookbook.prosemaker;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class PhraseStats {
    private String from;
    private Map<String, Integer> toCounts = new HashMap<String, Integer>();
    private int total;
    private Random random = new Random();

    public PhraseStats(String from) {
        this.from = from;
        this.total = 0;
    }

    public void increment(String to) {
        if (!toCounts.containsKey(to)) {
            toCounts.put(to, 1);
        } else {
            toCounts.put(to, toCounts.get(to) + 1);
        }
        total++;
    }

    public int getCount(String to) {
        return toCounts.get(to);
    }

    public int getTotalCount() {
        return total;
    }

    public String pickRandomTo() {
        int threshold = random.nextInt(total);
        int sum = 0;
        for (Map.Entry<String, Integer> entry : toCounts.entrySet()) {
            sum += entry.getValue();
            if (threshold <= sum) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException();
    }
}
