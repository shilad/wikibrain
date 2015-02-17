package org.wikibrain.sr;

import java.util.ArrayList;
import java.util.List;

/**
 * This class describes a result of a semantic relatedness calculation.
 * It contains the ID of the page compared to, a double representing the
 * SR score, and a list of explanations. The default SRResult is an ID of -1,
 * a score of Double.NaN, and no explanations. An ID of -1 always refers to
 * a null page, as in this case. An SRResult can also be constructed with
 * only a score, in which case the ID is set to -2, which means the score
 * has meaning but the ID is unspecified. Otherwise, constructors establish
 * both ID and score, with or without explanations.
 *
 * @author Matt Lesicko
 * @author Ari Weiland
 */
public class SRResult implements Comparable<SRResult>{
    protected int id;
    protected double score;
    protected List<Explanation> explanations;

    /**
     * Constructs a default SRResult that has no useful information.
     */
    public SRResult() {
        this(-1, Double.NaN);
    }

    /**
     * Constructs an SRResult that stores only a score. The -2 denotes
     * that it has meaning, but does not refer to a specific page.
     * @param score
     */
    public SRResult(double score) {
        this(-2, score);
    }

    /**
     * Constructs an SRResult with an ID, score, and empty explanations.
     * @param id
     * @param score
     */
    public SRResult(int id, double score) {
        this(id, score, new ArrayList<Explanation>());
    }

    /**
     * Constructs an SRResult with an ID, score, and explanations.
     * @param id
     * @param score
     * @param explanations
     */
    public SRResult(int id, double score, List<Explanation> explanations) {
        this.id = id;
        this.score = score;
        this.explanations = explanations;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getScore() {
        return score;
    }

    /**
     * Returns whether or not the score is valid,
     * ie. is an actual value.
     * @return false if and only if score is Double.NaN
     */
    public boolean isValid() {
        return !Double.isNaN(score) && !Double.isInfinite(score);
    }

    public void setScore(double score) {
        this.score = score;
    }

    public List<Explanation> getExplanations() {
        return explanations;
    }

    public void addExplanation(Explanation explanation) {
        explanations.add(explanation);
    }

    public void setExplanations(List<Explanation> explanations) {
        this.explanations = explanations;
    }

    /**
     * Zeros the score about the specified value.
     * Uncenterd values are centered around 0.
     * @param zeroValue
     */
    public void centerValue(double zeroValue){
        this.score = this.score - zeroValue;
        if (this.score < 0) this.score = 0.0;
    }

    @Override
    public int compareTo(SRResult result) {
        return ((Double)this.score).compareTo(result.getScore());
    }

    @Override
    public String toString() {
        return "SRResult{" +
                "id=" + id +
                ", score=" + score +
                '}';
    }
}
