package org.wikapidia.sr.disambig;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.LocalSRMetric;

import java.util.List;

public class SimilarityDisambiguator extends BaseDisambiguator{
    SimilarityDisambiguator(PhraseAnalyzer phraseAnalyzer, LocalSRMetric srMetric) {
        super(phraseAnalyzer, srMetric);
    }

    /**
     * Returns the cosimilarity matrix for the specified pages
     * @param pages
     * @return
     * @throws DaoException
     */
    @Override
    protected double[][] getCosimilarity(List<LocalPage> pages) throws DaoException {
        return srMetric.cosimilarity((int[]) null, null);
    }
}
