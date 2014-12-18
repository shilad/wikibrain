package org.wikibrain.utils;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import org.apache.commons.lang3.ArrayUtils;

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

    public static int[] sortMapKeys(final TIntFloatMap map, boolean reverse) {
        Integer keys[] = ArrayUtils.toObject(map.keys());
        Arrays.sort(keys, new Comparator<Integer>() {
            @Override
            public int compare(Integer k1, Integer k2) {
                Float v1 = map.get(k1);
                Float v2 = map.get(k2);
                return v1.compareTo(v2);
            }
        });
        if (reverse) {
            ArrayUtils.reverse(keys);
        }
        return ArrayUtils.toPrimitive(keys);
    }
    public static int[] sortMapKeys(final TIntDoubleMap map, boolean reverse) {
        Integer keys[] = ArrayUtils.toObject(map.keys());
        Arrays.sort(keys, new Comparator<Integer>() {
            @Override
            public int compare(Integer k1, Integer k2) {
                Double v1 = map.get(k1);
                Double v2 = map.get(k2);
                return v1.compareTo(v2);
            }
        });
        if (reverse) {
            ArrayUtils.reverse(keys);
        }
        return ArrayUtils.toPrimitive(keys);
    }

    public static int[] sortMapKeys(final TIntIntMap map) {
        return sortMapKeys(map, false);
    }

    public static int[] sortMapKeys(final TIntIntMap map, boolean reverse) {
        Integer keys[] = ArrayUtils.toObject(map.keys());
        Arrays.sort(keys, new Comparator<Integer>() {
            @Override
            public int compare(Integer k1, Integer k2) {
                return map.get(k1) - map.get(k2);
            }
        });
        if (reverse) {
            ArrayUtils.reverse(keys);
        }
        return ArrayUtils.toPrimitive(keys);
    }

    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortMap(final Map<K, V> map) {
        return sortMap(map, false);
    }

    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortMap(final Map<K, V> map, boolean reverse) {
        LinkedHashMap<K, V> sorted = new LinkedHashMap<K, V>();
        for (K key : sortMapKeys(map, reverse)) {
            sorted.put(key, map.get(key));
        }
        return sorted;
    }

    public static <T extends Comparable<T>> T max(Collection<T> elems) {
        T max = null;
        for (T t : elems) {
            if (max == null || max.compareTo(t) < 0) {
                max = t;
            }
        }
        return max;
    }

    /**
     * From http://stackoverflow.com/questions/3047051/how-to-determine-if-a-list-is-sorted-in-java
     * @param iterable
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> boolean isSorted(Iterable<T> iterable) {
        Iterator<T> iter = iterable.iterator();
        if (!iter.hasNext()) {
            return true;
        }
        T t = iter.next();
        while (iter.hasNext()) {
            T t2 = iter.next();
            if (t.compareTo(t2) > 0) {
                return false;
            }
            t = t2;
        }
        return true;
    }

    public static <T extends Comparable<T>> T min(Collection<T> elems) {
        T min = null;
        for (T t : elems) {
            if (min == null || min.compareTo(t) > 0) {
                min = t;
            }
        }
        return min;
    }
}
