package org.wikapidia.sr.ensemble;

import org.wikapidia.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Lesicko
 */
public class EnsembleSim {
    List<Double> scores;
    List<Integer> ranks;
    KnownSim knownSim;

    public EnsembleSim(List<Double> scores, List<Integer> ranks, KnownSim knownSim){
        this.scores=scores;
        this.ranks=ranks;
        this.knownSim=knownSim;
    }

    public EnsembleSim(KnownSim knownSim) {
        this.scores = new ArrayList<Double>();
        this.ranks = new ArrayList<Integer>();
        this.knownSim = knownSim;
    }

    public List<Double> getScores() {
        return scores;
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
