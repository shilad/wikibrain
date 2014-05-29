package org.wikibrain.sr.evaluation;

import org.wikibrain.sr.utils.KnownSim;

/**
 * @author Shilad Sen
 */
public class KnownSimGuess {
    private final KnownSim known;
    private final double guess;

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

    public double getError2() {
        if (!hasGuess()) {
            return Double.NaN;
        }
        return (guess - known.similarity) * (guess - known.similarity);
    }

    public double getActual() {
        return known.similarity;
    }
}
