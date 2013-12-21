package org.wikapidia.utils;

import java.util.*;

/**
 * @author Shilad Sen
 */
public class WpCollectionUtils {
    public static <K, V extends Comparable<V>> List<K> sortMapKeys(final Map<K, V> map, boolean reverse) {
        List<K> keys = new ArrayList<K>(map.keySet());
        Collections.sort(keys, new Comparator<K>() {
            @Override
            public int compare(K k1, K k2) {
                return map.get(k1).compareTo(map.get(k2));
            }
        });
        if (reverse) {
            Collections.reverse(keys);
        }
        return keys;
    }

    public static <K, V extends Comparable<V>> List<K> sortMapKeys(final Map<K, V> map) {
        return sortMapKeys(map, false);
    }
}
