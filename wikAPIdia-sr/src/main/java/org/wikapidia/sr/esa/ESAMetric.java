package org.wikapidia.sr.esa;

import com.typesafe.config.Config;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.lucene.search.Query;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
import org.wikapidia.lucene.WikapidiaScoreDoc;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.pairwise.PairwiseCosineSimilarity;
import org.wikapidia.sr.pairwise.PairwiseSimilarity;
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
    private boolean resolvePhrases;

    public ESAMetric(LuceneSearcher searcher, LocalPageDao pageHelper) {
        this.searcher = searcher;
        this.pageHelper = pageHelper;
        resolvePhrases = false;
    }

    public ESAMetric(LuceneSearcher searcher, LocalPageDao pageHelper, Disambiguator disambiguator){
        this.searcher = searcher;
        this.pageHelper = pageHelper;
        this.disambiguator = disambiguator;
        resolvePhrases = true;
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
        if (resolvePhrases){
            return super.mostSimilar(phrase,maxResults);
        }
        List<SRResult> results = new ArrayList<SRResult>();
        Language language = phrase.getLanguage();
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);

        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.search(queryBuilder.getPhraseQuery(phrase.getString()), language);

        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            int localPageId = searcher.getLocalIdFromDocId(wikapidiaScoreDoc.doc, language);
            SRResult result = new SRResult((double) wikapidiaScoreDoc.score);
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
        if (resolvePhrases){
            return super.similarity(phrase1,phrase2,language,explanations);
        }
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
        return normalize(result,language);
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
            WikapidiaScoreDoc[] scoreDocs = searcher.search(query, language);
            SimUtils.pruneSimilar(scoreDocs);
            return SimUtils.normalizeVector(expandScores(scoreDocs));
        } else {
            LOG.log(Level.WARNING, "Phrase cannot be parsed to get a query.");
            return null;
        }
    }

    /**
     * Get concept vector of a local page with a specified language.
     * @param id
     * @param language
     * @return
     * @throws DaoException
     */
    public TIntDoubleHashMap getVector(int id, Language language) throws DaoException { // TODO: validIDs
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);
//        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getLocalPageConceptQuery(localPage), language);
        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.search(queryBuilder.getMoreLikeThisQuery(searcher.getDocIdFromLocalId(id, language), searcher.getReaderByLanguage(language)), language);
        SimUtils.pruneSimilar(wikapidiaScoreDocs);
        return SimUtils.normalizeVector(expandScores(wikapidiaScoreDocs));
    }

    /**
     * Put data in a scoreDoc into a TIntDoubleHashMap
     *
     * @param wikapidiaScoreDocs
     * @return
     */
    private TIntDoubleHashMap expandScores(WikapidiaScoreDoc[] wikapidiaScoreDocs) {
        TIntDoubleHashMap expanded = new TIntDoubleHashMap();
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            expanded.put(wikapidiaScoreDoc.doc, wikapidiaScoreDoc.score);
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
        if (page1.getLanguage()!=page2.getLanguage()){
            throw new IllegalArgumentException("Tried to compute local similarity of pages in different languages: page1 was in"+page1.getLanguage().getEnLangName()+" and page2 was in "+ page2.getLanguage().getEnLangName());
        }
        TIntDoubleHashMap scores1 = getVector(page1.getLocalId(), page1.getLanguage());
        TIntDoubleHashMap scores2 = getVector(page2.getLocalId(), page2.getLanguage());
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
        return normalize(result, page1.getLanguage());
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
        return mostSimilar(localPage, maxResults, null);
    }

    public SRResultList mostSimilar(LocalPage localPage, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(localPage.getLanguage(), localPage.getLocalId())){
            SRResultList mostSimilar= getCachedMostSimilarLocal(localPage.getLanguage(), localPage.getLocalId(), maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            System.out.println("from cache!");
            return mostSimilar;
        }
        Language language = localPage.getLanguage();
        QueryBuilder queryBuilder = searcher.getQueryBuilderByLanguage(language);
        searcher.setHitCount(maxResults);
//        ScoreDoc[] scoreDocs = searcher.search(queryBuilder.getLocalPageConceptQuery(localPage), language);
        Query query = queryBuilder.getMoreLikeThisQuery(searcher.getDocIdFromLocalId(localPage.getLocalId(), language), searcher.getReaderByLanguage(language));
        System.out.println(query);
        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.search(query, language);
        SRResultList srResults = new SRResultList(maxResults);
        int i = 0;
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            if (i < srResults.numDocs()) {
                int localId = searcher.getLocalIdFromDocId(wikapidiaScoreDoc.doc, language);
                if (validIds==null||validIds.contains(localId)){
                    srResults.set(i, localId, wikapidiaScoreDoc.score);
                    i++;
                }
            }
        }
        return normalize(srResults,language);
    }

    public String getName() {
        return "ESA";
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        PairwiseSimilarity pairwiseSimilarity = new PairwiseCosineSimilarity();
        super.writeCosimilarity(path, languages, maxHits,pairwiseSimilarity);
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
            ESAMetric sr;
            if (config.getBoolean("resolvephrases")){
                sr = new ESAMetric(
                        searcher,
                        getConfigurator().get(LocalPageDao.class, config.getString("pageDao")),
                        getConfigurator().get(Disambiguator.class, config.getString("disambiguator"))
                );
            } else{
                sr = new ESAMetric(
                        searcher,
                        getConfigurator().get(LocalPageDao.class, config.getString("pageDao"))
                );
            }
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
