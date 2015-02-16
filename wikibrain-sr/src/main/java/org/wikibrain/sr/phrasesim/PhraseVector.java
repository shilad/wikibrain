package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Shilad Sen
 */
public class PhraseVector implements Serializable {
    final long ids[];
    final float vals[];

    public PhraseVector(TLongFloatHashMap map) {
        this.ids = map.keys();
        Arrays.sort(this.ids);
        vals = new float[ids.length];
        for (int i = 0; i < ids.length; i++) {
            vals[i] = map.get(ids[i]);
        }
    }


    public PhraseVector(TIntFloatMap map) {
        this.ids = new long[map.size()];
        int i = 0;
        for (int id : map.keys()) {
            ids[i++] = id;
        }
        Arrays.sort(this.ids);
        vals = new float[ids.length];
        for (i = 0; i < ids.length; i++) {
            vals[i] = map.get((int) ids[i]);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof  PhraseVector)) {
            return false;
        }
        PhraseVector that = (PhraseVector) other;
        return Arrays.equals(ids, that.ids) && Arrays.equals(vals, that.vals);
    }

    public static int sign(final float x) {
        return (x == 0.0f) ? 0 : (x > 0.0f) ? 1 : -1;
    }

    public static int sign(final long x) {
        return (x == 0L) ? 0 : (x > 0L) ? 1 : -1;
    }
}
