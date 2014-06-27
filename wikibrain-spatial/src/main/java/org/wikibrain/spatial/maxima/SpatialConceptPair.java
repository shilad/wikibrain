package org.wikibrain.spatial.maxima;

/**
 * Created by harpa003 on 6/27/14.
 */
public class SpatialConceptPair {
    private final SpatialConcept firstConcept;
    private final SpatialConcept secondConcept;
    private double distance;
    private double relatedness;

    public SpatialConceptPair(SpatialConcept firstConcept, SpatialConcept secondConcept) {
        this.firstConcept = firstConcept;
        this.secondConcept = secondConcept;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setRelatedness(double relatedness) {
        this.relatedness = relatedness;
    }

    public SpatialConcept getFirstConcept() {
        return firstConcept;
    }

    public SpatialConcept getSecondConcept() {
        return secondConcept;
    }

    public double getDistance() {
        return distance;
    }

    public double getRelatedness() {
        return relatedness;
    }
}
