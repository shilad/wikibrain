package org.wikapidia.sr.ensemble;

import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.utils.KnownSim;

import java.util.List;

/**
 * @author Matt Lesicko
 */
public interface Ensemble {
    void trainSimilarity(List<EnsembleSim> simList);
    void trainMostSimilar(List<EnsembleSim> simList);
    SRResult predictSimilarity(List<SRResult> scores);
    SRResult predictMostSimilar(List<SRResult> scores);
    void read(String file);
    void write(String file);
}
