package org.wikapidia.sr;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.SimUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author Yulun Li
*/
public class ESAMetric extends BaseLocalSRMetric {

    private static final Logger LOG = Logger.getLogger(ESAMetric.class.getName());

    private final LuceneSearcher searcher;

    public ESAMetric(LuceneSearcher searcher, LocalPageDao pageHelper) {
        this.searcher = searcher;
        this.pageHelper = pageHelper;
    }

    /**
     * Get the most similar Wikipedia pages of a specified localString.
     * @param phrase local string containing the language information
     * @param maxResults number of results returned
     * @return SRResulList
     * @throws DaoException
     */
    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException {
        List<SRResult> results = new ArrayList<SRResult>();
        Language language = phrase.getLanguage();
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);

        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getPhraseQuery(phrase.getString()), language);

        for (ScoreDoc scoreDoc : scoreDocs) {
            int localPageId = searcher.getLocalIdFromDocId(scoreDoc.doc, language);
            SRResult result = new SRResult((double) scoreDoc.score);
            result.id  = localPageId;
            results.add(result);
        }
        SRResultList resultList = new SRResultList(maxResults);
        for (int j = 0; j < maxResults && j < results.size(); j++){
            resultList.set(j, results.get(j));
        }
        return resultList;
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
    @Override
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
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);
        Query query = queryBuilder.getPhraseQuery(phrase);
        if (query != null) {
            ScoreDoc[] scoreDocs = searcher.search(query, language);
            pruneSimilar(scoreDocs);
            return SimUtils.normalizeVector(expandScores(scoreDocs));
        } else {
            LOG.log(Level.WARNING, "Phrase cannot be parsed to get a query.");
            return null;
        }
    }

    /**
     * Get concept vector of a local page with a specified language.
     * @param localPage
     * @param language
     * @return
     * @throws DaoException
     */
    private TIntDoubleHashMap getConceptVector(LocalPage localPage, Language language) throws DaoException { // TODO: validIDs
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);
//        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getLocalPageConceptQuery(localPage), language);
        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getMoreLikeThisQuery(searcher.getDocIdFromLocalId(localPage.getLocalId(), language), searcher.getReaderByLanguage(language)), language);
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
     * @return
     * @throws DaoException
     */
    public SRResultList mostSimilar(LocalPage localPage, int maxResults) throws DaoException {
        Language language = localPage.getLanguage();
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);
        searcher.setHitCount(maxResults);
//        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getLocalPageConceptQuery(localPage), language);
        Query query = queryBuilder.getMoreLikeThisQuery(searcher.getDocIdFromLocalId(localPage.getLocalId(), language), searcher.getReaderByLanguage(language));
        System.out.println(query);
        ScoreDoc[] scoreDocs = searcher.search(query, language);
        SRResultList srResults = new SRResultList(maxResults);
        int i = 0;
        for (ScoreDoc scoreDoc : scoreDocs) {
            if (i < srResults.numDocs()) {
                int localId = searcher.getLocalIdFromDocId(scoreDoc.doc, language);
                //TODO: normalize me!
                srResults.set(i, localId, scoreDoc.score);
                i++;
            }
        }
        return srResults;
    }

    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        return null;
    }

    public String getName() {
        return "ESA";
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalSRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public LocalSRMetric get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("ESA")) {
                return null;
            }

            List<String> langCodes = getConfig().get().getStringList("languages");

            LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(langCodes), getConfigurator().get(LuceneOptions.class, "esa"));
            ESAMetric sr = new ESAMetric(
                    searcher,
                    getConfigurator().get(LocalPageDao.class, config.getString("pageDao"))
            );
            try {
                sr.read(getConfig().get().getString("sr.metric.path"));
            } catch (IOException e){
                sr.setDefaultSimilarityNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                sr.setDefaultMostSimilarNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                for (String langCode : langCodes){
                    Language language = Language.getByLangCode(langCode);
                    sr.setSimilarityNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                    sr.setMostSimilarNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                }
            }

            for (String langCode : langCodes){
                try {
                    sr.readCosimilarity(getConfig().get().getString("sr.metric.path"), Language.getByLangCode(langCode));
                } catch (IOException e) {}
            }
            return sr;
        }

    }
}
