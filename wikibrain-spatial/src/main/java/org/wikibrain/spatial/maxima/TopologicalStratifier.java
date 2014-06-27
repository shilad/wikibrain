package org.wikibrain.spatial.maxima;

/**
 * Created by horla001 on 6/27/14.
 */
public class TopologicalStratifier extends SpatialConceptPairStratifier {

    private static final int tierOneSeparation = 2;
    private static final int tierTwoSeparation = 5;
    private static final int numBuckets = 3;

    @Override
    public int getStrata(SpatialConceptPair conceptPair) {
        int topoDistance = conceptPair.getTopDistance();
        if(topoDistance <= tierOneSeparation) {
            return 0;
        }

        return topoDistance <= tierTwoSeparation ? 1 : 2;
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
        return "Topological";
    }

    @Override
    public int getNumBuckets() {
        return numBuckets;
    }
}
