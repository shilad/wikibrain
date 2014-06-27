package org.wikibrain.spatial.maxima;

/**
 * Created by horla001 on 6/27/14.
 */
public class SemanticRelatednessStratifier extends SpatialConceptPairStratifier {

    private static final int numBuckets = 2;
    private static final double unrelatedCutoff = 0.5;

    @Override
    public int getStrata(SpatialConceptPair conceptPair) {
        return conceptPair.getRelatedness() < unrelatedCutoff ? 0 : 1;
    }

    @Override
    public double[] getDesiredStratification() {
        double[] strats = new double[numBuckets];

        strats[0] = strats[1] = 0.5;

        return strats;
    }

    @Override
    public String getName() {
        return "SemanticRelatedness";
    }

    @Override
    public int getNumBuckets() {
        return numBuckets;
    }
}
