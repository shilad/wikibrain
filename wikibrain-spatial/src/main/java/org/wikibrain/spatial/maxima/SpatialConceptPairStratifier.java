package org.wikibrain.spatial.maxima;

/**
 * Created by horla001 on 6/27/14.
 */
public abstract class SpatialConceptPairStratifier {

    /**
     * Determines in which strata a pair should be placed
     * @param conceptPair The concept pair in question
     * @return An integer that represents the unique index of the strata
     */
    public abstract int getStrata(SpatialConceptPair conceptPair);

    /**
     * Responsible for determining the target stratification
     * @return An array of doubles with each index representing the
     * distribution of the given bucket. Array values should sum to 1.
     */
    public abstract double[] getDesiredStratification();

    /**
     * Getter for name value
     * @return The name for the stratifier
     */
    public abstract String getName();

    /**
     * Calculates the number of buckets in the stratification
     * (Implementations should override this in a more efficient
     * manner).
     * @return An int representing the total possible buckets
     */
    public int getNumBuckets() {
        return getDesiredStratification().length;
    }

}
