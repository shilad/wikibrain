package org.wikibrain.utils;

import java.util.Comparator;
import java.util.Map;

public class MapValueComparator<K> implements Comparator<K> {
    private final boolean ascending;
    private Map<K, Comparable> base;

    public MapValueComparator(Map<K, Comparable> base) {
        this(base, true);
    }

    public MapValueComparator(Map<K, Comparable> base, boolean ascending) {
        this.ascending = ascending;
        this.base = base;
    }

    public int compare(K a, K b) {
        int sign = ascending ? 1 : -1;
        return sign * base.get(a).compareTo(base.get(b));
    }
}