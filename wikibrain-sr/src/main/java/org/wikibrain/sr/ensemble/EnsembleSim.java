package org.wikibrain.sr.ensemble;

import org.wikibrain.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Lesicko
 */
public class EnsembleSim {
    List<Double> scores;
    List<Integer> ranks;
    KnownSim knownSim;

    public EnsembleSim(KnownSim knownSim) {
        this.scores = new ArrayList<Double>();
        this.ranks = new ArrayList<Integer>();
        this.knownSim = knownSim;
    }

    public List<Double> getScores() {
        return scores;
    }

    public int getNumMetricsWithScore() {
        int n = 0;
        for (Double s : scores) {
            if (!Double.isNaN(s) && !Double.isInfinite(s)) {
                n++;
            }
        }
        return n;
    }

    public List<Integer> getRanks(){
        return ranks;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public KnownSim getKnownSim() {
        return knownSim;
    }

    public void add(double score, int rank) {
        scores.add(score);
        ranks.add(rank);
    }
}
