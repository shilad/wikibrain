package org.wikapidia.sr;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
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

    private final LuceneSearcher searcher;

    public ESAMetric(Language language, LuceneSearcher searcher, LocalPageDao pageHelper) {
        this.searcher = searcher;
        this.pageHelper = pageHelper;
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
        SRResult result = new SRResult(sim);

        if (explanations) {

            String format = "Five most similar pages to " + phrase1 + "" +
                    "\n?\n?\n?\n?\n?\nFive most similar pages to " + phrase2 + "\n?\n?\n?\n?\n?";
            List<LocalPage> formatPages =new ArrayList<LocalPage>();

            Map<Integer, Double> ids = SimUtils.sortByValue(scores1);
            int i = 0;
            for (int id : ids.keySet()) {
                if (i++ < 5) {
                    int localPageId = searcher.getLocalIdFromDocId(id, language);
                    LocalPage topPage = pageHelper.getById(language, localPageId);
                    if (topPage==null) {
                        continue;
                    }
                    formatPages.add(topPage);
                }
            }
            Map<Integer, Double> ids1 = SimUtils.sortByValue(scores2);
            int j = 0;
            for (int id : ids1.keySet()) {
                if (j++ < 5) {
                    int localPageId = searcher.getLocalIdFromDocId(id, language);
                    LocalPage topPage = pageHelper.getById(language, localPageId);
                    if (topPage==null) {
                        continue;
                    }
                    formatPages.add(topPage);
                }
            }
            Explanation explanation = new Explanation(format, formatPages);
            result.addExplanation(explanation);
        }

        return result; // TODO: normalize
    }

    /**
     * Get concept vector of a specified phrase.
     *
     * @param phrase
     * @return
     */
    public TIntDoubleHashMap getConceptVector(String phrase, Language language) throws DaoException { // TODO: validIDs
        try {
            QueryBuilder queryBuilder = new QueryBuilder(language, searcher.getOptions());
            ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getPhraseQuery(phrase), language);
            pruneSimilar(scoreDocs);
            return SimUtils.normalizeVector(expandScores(scoreDocs));
        } catch (ParseException e) {
            throw new DaoException(e);
        }
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
        TIntDoubleHashMap scores1 = getConceptVector(page1, page1.getLanguage());
        TIntDoubleHashMap scores2 = getConceptVector(page2, page2.getLanguage());
        double sim = SimUtils.cosineSimilarity(scores1, scores2);
        SRResult result = new SRResult(sim);

        if (explanations) {

            String format = "Five most similar pages to ?\n?\n?\n?\n?\n?\nFive most similar pages to ?\n?\n?\n?\n?\n?";
            List<LocalPage> formatPages =new ArrayList<LocalPage>();

            Map<Integer, Double> ids = SimUtils.sortByValue(scores1);
            formatPages.add(page1);
            int i = 0;
            for (int id : ids.keySet()) {
                if (i++ < 5) {
                    int localPageId = searcher.getLocalIdFromDocId(id, page1.getLanguage());
                    LocalPage topPage = pageHelper.getById(page1.getLanguage(), localPageId);
                    if (topPage==null) {
                        continue;
                    }
                    formatPages.add(topPage);
                }
            }
            formatPages.add(page2);
            Map<Integer, Double> ids1 = SimUtils.sortByValue(scores2);
            int j = 0;
            for (int id : ids1.keySet()) {
                if (j++ < 5) {
                    int localPageId = searcher.getLocalIdFromDocId(id, page2.getLanguage());
                    LocalPage topPage = pageHelper.getById(page2.getLanguage(), localPageId);
                    if (topPage==null) {
                        continue;
                    }
                    formatPages.add(topPage);
                }
            }
            Explanation explanation = new Explanation(format, formatPages);
            result.addExplanation(explanation);
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
        Language language = localPage.getLanguage();
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
            String format = "?'s similar pages include ?";
            for (SRResult srResult : srResults) {
                if (srResult.getValue() != 0) {
                    List<LocalPage> formatPages =new ArrayList<LocalPage>();
                    int localPageId = searcher.getLocalIdFromDocId(srResult.id, language);
                    LocalPage topPage = pageHelper.getById(language, localPageId);
                    if (topPage==null) {
                        continue;
                    }
                    formatPages.add(localPage);
                    formatPages.add(topPage);
                    Explanation explanation = new Explanation(format, formatPages);
                    srResult.addExplanation(explanation);
                }
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
