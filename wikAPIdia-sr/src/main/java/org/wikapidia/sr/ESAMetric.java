package org.wikapidia.sr;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.lucene.search.ScoreDoc;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Yulun Li
 */
public class ESAMetric extends BaseLocalSRMetric {

    // TODO: test ESA independently
    // TODO: finish article similarity

    public String getName() {
        return "ESA";
    }

    private LuceneSearcher searcher;
    private static final Logger LOG = Logger.getLogger(ESAMetric.class.getName());

    /**
     * Get cosine similarity between two phrases.
     * @param phrase1
     * @param phrase2
     * @param language
     * @param explanations
     * @return
     * @throws DaoException
     */
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        if (phrase1 == null || phrase2 == null) {
            throw new NullPointerException("Null phrase passed to similarity");
        }
        try {
            TIntDoubleHashMap scores1 = getConceptVector(phrase1, language);
            TIntDoubleHashMap scores2 = getConceptVector(phrase2, language);
            double sim = SimUtils.cosineSimilarity(scores1, scores2);
            return new SRResult(sim); // TODO: normalize
        } catch (WikapidiaException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Get concept vector of a specified phrase.
     * @param phrase
     * @return
     */
    public TIntDoubleHashMap getConceptVector(String phrase, Language language) throws WikapidiaException { // TODO: validIDs
        ScoreDoc[] scoreDocs = searcher.search(phrase, language);
        // TODO: prune
        // normalize vector to unit length
        TIntDoubleHashMap result = SimUtils.normalizeVector(expandScores(scoreDocs));
        return result;
    }

    /**
     * Put data in a scoreDoc into a TIntDoubleHashMap
     * @param scores
     * @return
     */
    private TIntDoubleHashMap expandScores(ScoreDoc scores[]) {
        TIntDoubleHashMap expanded = new TIntDoubleHashMap();
        for (ScoreDoc sd : scores) {
            expanded.put(sd.doc, sd.score);
        }
        return expanded;
    }

    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void write(File directory) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void read(File directory) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainSimilarity(List<KnownSim> labeled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainMostSimilar(List<KnownSim> labeled, int numResults, TIntSet validIds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(String[] phrases, Language language) {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
