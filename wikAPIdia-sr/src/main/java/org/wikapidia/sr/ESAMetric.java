package org.wikapidia.sr;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
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

    private static final Logger LOG = Logger.getLogger(ESAMetric.class.getName());

    private Language language;
    private LuceneSearcher searcher;

    public ESAMetric(Language language) {
        this.language = language;
        searcher = new LuceneSearcher(new LanguageSet(language.getLangCode()), LuceneOptions.getDefaultOptions());
    }

    /**
     * Get cosine similarity between two phrases.
     *
     * @param phrase1
     * @param phrase2
     * @param language
     * @param explanations
     * @return
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
    public TIntDoubleHashMap getConceptVector(String phrase, Language language) { // TODO: validIDs
        QueryBuilder queryBuilder = new QueryBuilder(language, searcher.getOptions());
        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getPhraseQuery(phrase), language);
        return SimUtils.normalizeVector(expandScores(scoreDocs));
        // TODO: prune
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
        // TODO: prune
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
        return new SRResult(sim); // TODO: normalize
    }

    /**
     * Get wiki pages that are the most similar to the specified local page.
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
        return srResults;
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
