package org.wikapidia.sr;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.search.ScoreDoc;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Yulun Li
 */
public class ESAMetric extends BaseLocalSRMetric {

    private static final Logger LOG = Logger.getLogger(ESAMetric.class.getName());

    private final Language language;
    private final LuceneSearcher searcher;

    protected LocalPageDao pageHelper;

    public ESAMetric(Language language, LuceneSearcher searcher) {
        this.language = language;
        this.searcher = searcher;
    }

    /**
     * Get cosine similarity between two phrases.
     *
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
        TIntDoubleHashMap scores1 = getConceptVector(phrase1, language);
        TIntDoubleHashMap scores2 = getConceptVector(phrase2, language);
        double sim = SimUtils.cosineSimilarity(scores1, scores2);
        return new SRResult(sim); // TODO: normalize
    }

    /**
     * Get concept vector of a specified phrase.
     *
     * @param phrase
     * @return
     */
    public TIntDoubleHashMap getConceptVector(String phrase, Language language) throws DaoException { // TODO: validIDs
        QueryBuilder queryBuilder = new QueryBuilder(language, searcher.getOptions());
        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getPhraseQuery(phrase), language);
        pruneSimilar(scoreDocs);
        return SimUtils.normalizeVector(expandScores(scoreDocs));
    }

    /**
     * Get concept vector of a local page with a specified language.
     * @param localPage
     * @param language
     * @return
     * @throws DaoException
     */
    public TIntDoubleHashMap getConceptVector(LocalPage localPage, Language language) throws DaoException { // TODO: validIDs
        QueryBuilder queryBuilder = new QueryBuilder(language, searcher.getOptions());
        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getLocalPageConceptQuery(localPage), language);
        pruneSimilar(scoreDocs);
        return SimUtils.normalizeVector(expandScores(scoreDocs));
    }

    /**
     * Put data in a scoreDoc into a TIntDoubleHashMap
     *
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

    /**
     * Get similarity between two local pages.
     *
     * @param page1
     * @param page2
     * @param explanations
     * @return
     * @throws DaoException
     */
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        TIntDoubleHashMap scores1 = getConceptVector(page1, language);
        TIntDoubleHashMap scores2 = getConceptVector(page2, language);
        double sim = SimUtils.cosineSimilarity(scores1, scores2);
        SRResult result = new SRResult(sim);

        if (explanations) {
            Map<Integer, Double>ids = SimUtils.sortByValue(scores1);
            String format = "Top pages for ? include ?";
            List<LocalPage> formatPages =new ArrayList<LocalPage>();
            for (int id : ids.keySet()) {
                LocalPage topPage = pageHelper.getById(language, id);
                if (topPage==null) {
                    continue;
                }
                formatPages.add(page1);
                formatPages.add(topPage);
                Explanation explanation = new Explanation(format, formatPages);
                result.addExplanation(explanation);
            }
        }

        return result; // TODO: normalize
    }

    /**
     * Get wiki pages that are the most similar to the specified local page.
     *
     * @param localPage
     * @param maxResults
     * @param explanations
     * @return
     * @throws DaoException
     */
    public SRResultList mostSimilar(LocalPage localPage, int maxResults, boolean explanations) throws DaoException {
        QueryBuilder queryBuilder = new QueryBuilder(language, searcher.getOptions());
        searcher.setHitCount(maxResults);
        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getLocalPageConceptQuery(localPage), language);
        SRResultList srResults = new SRResultList(maxResults);
        int i = 0;
        for (ScoreDoc scoreDoc : scoreDocs) {
            if (i < srResults.numDocs()) {
                srResults.set(i, scoreDoc.doc, scoreDoc.score);
                i++;
            }
        }

        if (explanations) {
            String format = "? is a top page of ?";
            List<LocalPage> formatPages =new ArrayList<LocalPage>();
            for (SRResult srResult : srResults) {
                LocalPage topPage = pageHelper.getById(language, srResult.id);
                if (topPage==null) {
                    continue;
                }
                formatPages.add(topPage);
                formatPages.add(localPage);
                Explanation explanation = new Explanation(format, formatPages);
                srResult.addExplanation(explanation);
            }
        }

        return srResults;
    }

    private void pruneSimilar(ScoreDoc[] scoreDocs) {
        if (scoreDocs.length == 0) {
            return;
        }
        int cutoff = scoreDocs.length;
        double threshold = 0.005 * scoreDocs[0].score;
        for (int i = 0, j = 100; j < scoreDocs.length; i++, j++) {
            float delta = scoreDocs[i].score - scoreDocs[j].score;
            if (delta < threshold) {
                cutoff = j;
                break;
            }
        }
        if (cutoff < scoreDocs.length) {
//            LOG.info("pruned results from " + docs.scoreDocs.length + " to " + cutoff);
            scoreDocs = ArrayUtils.subarray(scoreDocs, 0, cutoff);
        }
    }

    @Override
    public SRResultList mostSimilar(LocalPage localPage, int maxResults, boolean explanations, TIntSet validIds) throws DaoException {
        return null;
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

    public String getName() {
        return "ESA";
    }
}
