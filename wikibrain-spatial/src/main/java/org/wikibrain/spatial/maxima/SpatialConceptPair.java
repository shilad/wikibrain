package org.wikibrain.spatial.maxima;

/**
 * Created by harpa003 on 6/27/14.
 */
public class SpatialConceptPair {
    private final SpatialConcept firstConcept;
    private final SpatialConcept secondConcept;
    private double kmDistance;
    private int topDistance;
    private double relatedness;
    private int kkTypeNumbOfTimesAsked;
    private int uuTypeNumbOfTimesAsked;
    private int kuTypeNumbOfTimesAsked;
    private double score;

    public SpatialConceptPair(SpatialConcept firstConcept, SpatialConcept secondConcept) {
        this.firstConcept = firstConcept;
        this.secondConcept = secondConcept;
    }

    public void setKmDistance(double kmDistance) {
        this.kmDistance = kmDistance;
    }

    public void setTopDistance(int topDistance) {
        this.topDistance = topDistance;
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

    public int getTopDistance() {
        return topDistance;
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

    public int getKuTypeNumbOfTimesAsked() {
        return kuTypeNumbOfTimesAsked;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
