package org.wikibrain.spatial.maxima;

/**
 * Created by harpa003 on 7/11/14.
 */
public class SemanticRelatednessStraddleStratifier extends SemanticRelatednessStratifier {

    @Override
    public double[] getDesiredStratification() {
        double[] strats = new double[numBuckets];

        strats[0] = strats[2] = 0.35;
        strats[1] = 0.3;

        return strats;
    }
}
