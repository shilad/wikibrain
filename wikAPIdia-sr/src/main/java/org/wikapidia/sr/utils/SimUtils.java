package org.wikapidia.sr.utils;

import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 *
 *
 */
public class SimUtils {

    public static double cosineSimilarity(TIntDoubleHashMap X, TIntDoubleHashMap Y) {
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;

        for (int id : X.keys()) {
            double x = X.get(id);
            xDotX += x * x;
            if (Y.containsKey(id)) {
                xDotY += x * Y.get(id);
            }
        }

        for (double y : Y.values()) {
            yDotY += y * y;
        }

        return xDotX * yDotY != 0 ? xDotY / Math.sqrt(xDotX * yDotY): Double.NaN;
    }
}
