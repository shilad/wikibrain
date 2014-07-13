package org.wikibrain.spatial.maxima;

/**
 * Created by horla001 on 6/27/14.
 */
public class SemanticRelatednessStratifier extends SpatialConceptPairStratifier {

    protected static final int numBuckets = 3;
    private static final double minRelatedCutoff = 0.33;
    private static final double maxRelatedCutoff = 0.66;

    @Override
    public int getStrata(SpatialConceptPair conceptPair) {
        double sr = conceptPair.getRelatedness();
        if(sr < minRelatedCutoff)
            return 0;

        return sr < maxRelatedCutoff ? 1 : 2;
    }

    @Override
    public double[] getDesiredStratification() {
        double[] strats = new double[numBuckets];

        strats[2] = 0.45;
        strats[1] = 0.35;
        strats[0] = 0.20;

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
