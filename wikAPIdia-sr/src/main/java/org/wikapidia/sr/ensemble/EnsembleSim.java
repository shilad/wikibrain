package org.wikapidia.sr.ensemble;

import org.wikapidia.sr.utils.KnownSim;

import java.util.List;

/**
 * @author Matt Lesicko
 */
public class EnsembleSim {
    List<Double> scores;
    KnownSim knownSim;

    public EnsembleSim(List<Double> scores, KnownSim knownSim){
        this.scores=scores;
        this.knownSim=knownSim;
    }

    public List<Double> getScores() {
        return scores;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public KnownSim getKnownSim() {
        return knownSim;
    }

    public void setKnownSim(KnownSim knownSim) {
        this.knownSim = knownSim;
    }
}
