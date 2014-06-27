package org.wikibrain.spatial.maxima;

/**
 * Created by harpa003 on 6/27/14.
 */
public class SpatialConceptPair {
    private final SpatialConcept firstConcept;
    private final SpatialConcept secondConcept;
    private double distance;
    private double relatedness;
    private int knownTypeNumbOfTimesAsked;
    private int unKnownTypeNumbOfTimesAsked;

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

    public void setKnownTypeNumbOfTimesAsked(int knownTypeNumbOfTimesAsked) {
        this.knownTypeNumbOfTimesAsked = knownTypeNumbOfTimesAsked;
    }

    public void setUnKnownTypeNumbOfTimesAsked(int unKnownTypeNumbOfTimesAsked) {
        this.unKnownTypeNumbOfTimesAsked = unKnownTypeNumbOfTimesAsked;
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

    public int getKnownTypeNumbOfTimesAsked() {
        return knownTypeNumbOfTimesAsked;
    }

    public int getUnKnownTypeNumbOfTimesAsked() {
        return unKnownTypeNumbOfTimesAsked;
    }
}
