package org.wikibrain.sr.phrasesim;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TLongFloatHashMap;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Shilad Sen
 */
public class PhraseVector implements Serializable {
    final long ids[];
    final float vals[];

    public PhraseVector(TLongFloatMap map) {
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

    public double cosineSim(PhraseVector that) {
        int n1 = this.ids.length;
        int n2 = that.ids.length;

        double xDotX = 0.0;
        double xDotY = 0.0;
        double yDotY = 0.0;
        int i = 0;
        int j = 0;
        while (i < n1 || j < n2) {
            if (i >= n1) {
                yDotY += that.vals[j] * that.vals[j];
                j++;
            } else if (j >= n2) {
                xDotX += this.vals[i] * this.vals[i];
                i++;
            } else if (this.ids[i] < that.ids[j]) {
                xDotX += this.vals[i] * this.vals[i];
                i++;
            } else if (this.ids[i] > that.ids[j]) {
                yDotY += that.vals[j] * that.vals[j];
                j++;
            } else {
                xDotX += this.vals[i] * this.vals[i];
                yDotY += that.vals[j] * that.vals[j];
                xDotY += this.vals[i] * that.vals[j];
                i++;
                j++;
            }
        }
        if (xDotX == 0.0 || yDotY == 0.0) {
            return 0.0;
        } else {
            return xDotY / Math.sqrt(xDotX * yDotY);
        }
    }

    public double norm2() {
        double sum2 = 0.0;
        for (int i = 0; i < vals.length; i++) {
            sum2 += vals[i] * vals[i];
        }
        return Math.sqrt(sum2);
    }
}
