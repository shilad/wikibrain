package org.wikibrain.sr.word2vec;

import org.wikibrain.utils.WpStringUtils;

/**
 * @author Shilad Sen
 */
public class Word2VecUtils {
    /**
     * Returns a hashcode for a particular word.
     * The hashCode 0 will NEVER be returned.
     * @param w
     * @return
     */
    public static long hashWord(String w) {
        long h = WpStringUtils.longHashCode(w);
        if (h == 0) h = 1;  // hack: h == 0 is reserved.
        return h;
    }
}
