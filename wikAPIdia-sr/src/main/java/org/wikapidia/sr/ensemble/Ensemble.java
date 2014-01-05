package org.wikapidia.sr.ensemble;

import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;

import java.io.IOException;
import java.util.List;

/**
 * @author Matt Lesicko
 */
public interface Ensemble {
    void trainSimilarity(List<EnsembleSim> simList);
    void trainMostSimilar(List<EnsembleSim> simList);
    SRResult predictSimilarity(List<SRResult> scores);
    SRResultList predictMostSimilar(List<SRResultList> scores, int maxResults);
    void read(String file) throws IOException;
    void write(String file) throws IOException;
}
