package org.wikibrain.spatial.maxima;

import org.wikibrain.core.cmd.Env;
import org.wikibrain.matrix.Matrix;
import org.wikibrain.spatial.cookbook.tflevaluate.MatrixGenerator;

import java.util.Map;

/**
 * Created by harpa003 on 6/27/14.
 */
public class SpatialConceptPair {
    private final SpatialConcept firstConcept;
    private final SpatialConcept secondConcept;
    private double kmDistance;
    private float graphDistance;
    private double relatedness;
    private int kkTypeNumbOfTimesAsked;
    private int uuTypeNumbOfTimesAsked;
    private int kuTypeNumbOfTimesAsked;

    public SpatialConceptPair(SpatialConcept firstConcept, SpatialConcept secondConcept) {
        this.firstConcept = firstConcept;
        this.secondConcept = secondConcept;
    }

    public void setKmDistance(double kmDistance) {
        this.kmDistance = kmDistance;
    }

    public void setGraphDistance(float graphDistance) {
        this.graphDistance = graphDistance;
    }

    public void setRelatedness(double relatedness) {
        this.relatedness = relatedness;
    }

    public void setKkTypeNumbOfTimesAsked(int kkTypeNumbOfTimesAsked) {
        this.kkTypeNumbOfTimesAsked = kkTypeNumbOfTimesAsked;
    }

    public void increaseKkNumbOfTimesAsked(int increase){
        kkTypeNumbOfTimesAsked=kkTypeNumbOfTimesAsked+increase;
    }

    public void setUuTypeNumbOfTimesAsked(int uuTypeNumbOfTimesAsked) {
        this.uuTypeNumbOfTimesAsked = uuTypeNumbOfTimesAsked;
    }

    public void increaseUuNumbOfTimesAsked(int increase){
        uuTypeNumbOfTimesAsked=uuTypeNumbOfTimesAsked+increase;
    }

    public void setKuTypeNumbOfTimesAsked(int kuTypeNumbOfTimesAsked) {
        this.kuTypeNumbOfTimesAsked = kuTypeNumbOfTimesAsked;
    }

    public void increaseKuNumbOfTimesAsked(int increase){
        kuTypeNumbOfTimesAsked=kuTypeNumbOfTimesAsked+increase;
    }

    public SpatialConcept getFirstConcept() {
        return firstConcept;
    }

    public SpatialConcept getSecondConcept() {
        return secondConcept;
    }

    public double getKmDistance() {
        return kmDistance;
    }

    public float getGraphDistance() {
        return graphDistance;
    }

    public double getRelatedness() {
        return relatedness;
    }

    public int getkkTypeNumbOfTimesAsked() {
        return kkTypeNumbOfTimesAsked;
    }

    public int getuuTypeNumbOfTimesAsked() {
        return uuTypeNumbOfTimesAsked;
    }

    public int getkuTypeNumbOfTimesAsked() {
        return kuTypeNumbOfTimesAsked;
    }

    private String getCombinedTitle() {
        String firstStr = firstConcept.getTitle();
        String secondStr = secondConcept.getTitle();

        String combinedTitle = null;
        try {
            if (firstStr.compareTo(secondStr) < 0) {
                combinedTitle = firstStr + secondStr;
            } else {
                combinedTitle = secondStr + firstStr;
            }
        } catch(NullPointerException e){
            System.out.println(firstConcept.getUniversalID());
            System.out.println(secondConcept.getUniversalID());
        }

        return combinedTitle;
    }

    @Override
    public int hashCode() {
        return getCombinedTitle().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        }

        if(other.getClass() != this.getClass()) {
            return false;
        }
        SpatialConceptPair opair = (SpatialConceptPair)other;

        int mid1 = firstConcept.getUniversalID();
        int mid2 = secondConcept.getUniversalID();
        int oid1 = opair.getFirstConcept().getUniversalID();
        int oid2 = opair.getSecondConcept().getUniversalID();

        return (mid1 == oid1 && mid2 == oid2) || (mid1 == oid2 && mid2 == oid1);
    }
}
