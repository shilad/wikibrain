package org.wikibrain.sr.ensemble;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class Interpolator implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(Interpolator.class);
    private final int numMetrics;
    private int[] missingRanks;
    private double[] missingScores;

    public Interpolator(int numMetrics) {
        this.numMetrics = numMetrics;
        missingRanks = new int[numMetrics];
        missingScores = new double[numMetrics];
        Arrays.fill(missingRanks, 1000);
        Arrays.fill(missingScores, 0.0);
    }

    /**
     * calculate interpolated values for missing ranks and scores
     * @param examples
     */
    public void trainSimilarity(List<EnsembleSim> examples) {
        for (int i = 0; i < numMetrics; i++) {
            int numMissingScores = 0;
            double sumMissingScores = 0.0;
            for (EnsembleSim es : examples) {
                if (es != null) {
                    double v = es.getScores().get(i);
                    if (Double.isNaN(v) || Double.isInfinite(v)) {
                        sumMissingScores += es.getKnownSim().similarity;
                        numMissingScores++;
                    }
                }
            }
            missingScores[i] = (numMissingScores > 0) ? (sumMissingScores / numMissingScores) : 0.0;
            LOG.info("for metric " + i + ", " +
                    " estimated missing score " + missingScores[i]);
        }
    }

    /**
     * TODO: train similarity should use mean, not min.
     * calculate interpolated values for missing ranks and scores
     * @param examples
     */
    public void trainMostSimilar(List<EnsembleSim> examples) {
        for (int i = 0; i < numMetrics; i++) {
            int maxMissingRanks = -1;
            double maxScore = -1;
            double minScore = 100;
            for (EnsembleSim es : examples) {
                if (es != null && es.getScores() != null) {
                    double v = es.getScores().get(i);
                    if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                        maxScore = Math.max(maxScore, v);
                        minScore = Math.min(minScore, v);
                    }
                    maxMissingRanks = Math.max(maxMissingRanks, es.getRanks().get(i));
                }
            }
            missingRanks[i] = Math.max(100, maxMissingRanks * 5 / 4);
            missingScores[i] = minScore;
            LOG.info("for metric " + i + ", " +
                    " estimated missing rank " + missingRanks[i] +
                    " and missing score " + missingScores[i]);
        }
    }

    public EnsembleSim interpolate(EnsembleSim example) {
        EnsembleSim result = new EnsembleSim(example.knownSim);
        for (int i = 0; i < numMetrics; i++) {
            double v = example.getScores().get(i);
            int r = example.getRanks().get(i);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                v = missingScores[i];
            }
            if (r < 0) {
                r = missingRanks[i];
            }
            result.add(v, r);
        }
        return result;
    }

    public double interpolateScore(int metricIndex, double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return missingScores[metricIndex];
        } else {
            return score;
        }
    }

    public int interpolateRank(int metricIndex, int rank) {
        if (rank < 0) {
            return missingRanks[metricIndex];
        } else {
            return rank;
        }
    }

    public double getInterpolatedScore(int metricIndex) {
        return missingScores[metricIndex];
    }

    public int getInterpolatedRank(int metricIndex) {
        return missingRanks[metricIndex];
    }
}
