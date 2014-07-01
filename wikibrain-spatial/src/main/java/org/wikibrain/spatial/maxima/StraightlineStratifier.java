package org.wikibrain.spatial.maxima;

/**
 * Created by horla001 on 6/27/14.
 */
public class StraightlineStratifier extends SpatialConceptPairStratifier {

    private static final long tierOneDistance = 100;
    private static final long tierTwoDistance = 500;
    private static final int  numBuckets = 3;

    @Override
    public int getStrata(SpatialConceptPair conceptPair) {
        double distance = conceptPair.getKmDistance();

        if(distance < tierOneDistance) {
            return 0;
        }

        return distance < tierTwoDistance ? 1 : 2;
    }

    @Override
    public double[] getDesiredStratification() {
        double[] strat = new double[numBuckets];

        strat[0] = 0.4;
        strat[1] = 0.35;
        strat[2] = 0.25;

        return strat;
    }

    @Override
    public String getName() {
        return "Straightline";
    }

    @Override
    public int getNumBuckets() {
        return numBuckets;
    }
}
