package org.wikibrain.sr.evaluation;

/**
* @author Shilad Sen
*/
public class PrecisionRecallAccumulator {
    private int n;                   // at rank
    private double threshold;         // threshold for relevancy
    private int retrievedRelevant;    // number of known relevant phrases in returned results
    private int retrievedIrrelevant;  // number of known irrelevant phrases in returned results
    private int totalRelevant;        // total number of relevant pairs
    private int totalIrrelevant;      // total number of irrelevant pairs
    private double relevanceSum;

    public PrecisionRecallAccumulator(int n, double threshold) {
        this.n = n;
        this.threshold = threshold;
    }

    public void observeRetrieved(double relevance) {
        relevanceSum += relevance;
        if (relevance >= threshold) {
            retrievedRelevant++;
        } else {
            retrievedIrrelevant++;
        }
    }

    public void observe(double relevance) {
        if (relevance >= threshold) {
            totalRelevant++;
        } else {
            totalIrrelevant++;
        }
    }

    public void merge(PrecisionRecallAccumulator pr) {
        this.retrievedIrrelevant += pr.retrievedIrrelevant;
        this.retrievedRelevant += pr.retrievedRelevant;
        this.totalIrrelevant += pr.totalIrrelevant;
        this.totalRelevant += pr.totalRelevant;
        this.relevanceSum += pr.relevanceSum;
    }

    public double getPrecision() {
        return 1.0 * retrievedRelevant / (retrievedRelevant + retrievedIrrelevant);
    }

    public double getRecall() {
        return 1.0 * retrievedRelevant / totalRelevant;
    }

    public int getN() {
        return n;
    }

    public double getThreshold() {
        return threshold;
    }

    public int getRetrievedRelevant() {
        return retrievedRelevant;
    }

    public int getRetrievedIrrelevant() {
        return retrievedIrrelevant;
    }

    public int getTotalRelevant() {
        return totalRelevant;
    }

    public int getTotalIrrelevant() {
        return totalIrrelevant;
    }

    public double getMeanRelevance() {
        return relevanceSum / (retrievedRelevant + retrievedIrrelevant);
    }
}
