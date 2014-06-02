package org.wikibrain.sr.evaluation;

import org.wikibrain.sr.utils.KnownSim;

/**
 * @author Shilad Sen
 */
public class KnownSimGuess {
    private final KnownSim known;
    private final double guess;
    private double predictedRank;
    private double actualRank;

    public KnownSimGuess(KnownSim known, double guess) {
        this.known = known;
        this.guess = guess;
    }

    public boolean hasGuess() {
        return !Double.isNaN(guess) && !Double.isInfinite(guess);
    }

    public String getPhrase1() {
        return known.phrase1;
    }

    public String getPhrase2() {
        return known.phrase2;
    }

    public KnownSim getKnown() {
        return known;
    }

    public double getGuess() {
        return guess;
    }

    public double getError() {
        if (!hasGuess()) {
            return Double.NaN;
        }
        return guess - known.similarity;
    }

    public String getUniqueKey() {
        if (getPhrase1().compareTo(getPhrase2()) < 0) {
            return getPhrase1() + "|" + getPhrase2();
        } else {
            return getPhrase2() + "|" + getPhrase1();
        }
    }

    public double getError2() {
        if (!hasGuess()) {
            return Double.NaN;
        }
        return (guess - known.similarity) * (guess - known.similarity);
    }

    public double getActual() {
        return known.similarity;
    }

    public double getPredictedRank() {
        return predictedRank;
    }

    public void setPredictedRank(double predictedRank) {
        this.predictedRank = predictedRank;
    }

    public double getActualRank() {
        return actualRank;
    }

    public double getRankError() {
        return predictedRank - actualRank;
    }

    public void setActualRank(double actualRank) {
        this.actualRank = actualRank;
    }
}
