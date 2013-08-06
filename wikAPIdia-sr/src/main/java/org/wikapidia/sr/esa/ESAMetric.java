package org.wikapidia.sr.esa;

import com.typesafe.config.Config;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.*;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.IdentityNormalizer;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.pairwise.PairwiseCosineSimilarity;
import org.wikapidia.sr.pairwise.PairwiseSimilarity;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.SimUtils;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author Yulun Li
 *@author Matt Lesicko
 *@author Ben Hillmann
*/
public class ESAMetric extends BaseLocalSRMetric {
    private int POOL_SIZE = 10;

    private static final Logger LOG = Logger.getLogger(ESAMetric.class.getName());

    private final LuceneSearcher searcher;
    private boolean resolvePhrases;
    private Map<Language, WpIdFilter> conceptFilter = new HashMap<Language, WpIdFilter>();

    public ESAMetric(LuceneSearcher searcher, LocalPageDao pageHelper, Disambiguator disambiguator, boolean resolvePhrases) {
        this.searcher = searcher;
        this.pageHelper = pageHelper;
        this.disambiguator = disambiguator;
        this.resolvePhrases = resolvePhrases;
    }

    public void setConcepts(File dir) throws IOException {
        conceptFilter.clear();
        if (!dir.isDirectory()) {
            LOG.warning("concept path " + dir + " not a directory; defaulting to all concepts");
        }
        for (String file : dir.list()) {
            String langCode = FilenameUtils.getBaseName(file);
            TIntSet ids = new TIntHashSet();
            for (String wpId : FileUtils.readLines(new File(dir, file))) {
                ids.add(Integer.valueOf(wpId));
            }
            conceptFilter.put(Language.getByLangCode(langCode), new WpIdFilter(ids.toArray()));
            LOG.warning("installed " + ids.size() + " concepts for " + langCode);
        }
    }

    /**
     * Get the most similar Wikipedia pages of a specified localString.
     * If the matrix was not built, this may not find the highest scores,
     * but should find a set that is fairly close.
     * @param phrase local string containing the language information
     * @param maxResults number of results returned
     * @return SRResulList
     * @throws DaoException
     */
    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException {
        return mostSimilar(phrase, maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        if (resolvePhrases){
            return super.mostSimilar(phrase,maxResults);
        }
        Language language = phrase.getLanguage();
        WikapidiaScoreDoc[] wikapidiaScoreDocs = getQueryBuilderByLanguage(language)
                                            .setPhraseQuery(phrase.getString())
                                            .setNumHits(maxResults*POOL_SIZE)
                                            .search();

        TIntDoubleHashMap vector = getVector(phrase.getString(),phrase.getLanguage());
        List<SRResult> results = new ArrayList<SRResult>();

        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {

            int localPageId = searcher.getLocalIdFromDocId(wikapidiaScoreDoc.doc, language);
            if (validIds==null||validIds.contains(localPageId)){
                TIntDoubleHashMap comparison = getVector(localPageId, phrase.getLanguage());
                SRResult result = new SRResult(localPageId, SimUtils.cosineSimilarity(vector,comparison));
                results.add(result);
            }

        }
        Collections.sort(results);
        Collections.reverse(results);
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
        TIntDoubleHashMap scores1 = getVector(phrase1, language);
        TIntDoubleHashMap scores2 = getVector(phrase2, language);
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
    public TIntDoubleHashMap getVector(String phrase, Language language) throws DaoException { // TODO: validIDs
        QueryBuilder builder = getQueryBuilderByLanguage(language)
                                    .setPhraseQuery(phrase);
        if (builder.hasQuery()) {
            WikapidiaScoreDoc[] scoreDocs = builder.search();
            scoreDocs = SimUtils.pruneSimilar(scoreDocs);
            return SimUtils.normalizeVector(expandScores(scoreDocs));
        } else {
            LOG.log(Level.WARNING, "Phrase cannot be parsed to get a query. "+phrase);
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
    public TIntDoubleHashMap getVector(int id, Language language) throws DaoException {
        int luceneId = searcher.getDocIdFromLocalId(id, language);
        WikapidiaScoreDoc[] wikapidiaScoreDocs =  getQueryBuilderByLanguage(language)
                                .setMoreLikeThisQuery(luceneId)
                                .search();
        wikapidiaScoreDocs = SimUtils.pruneSimilar(wikapidiaScoreDocs);
        return SimUtils.normalizeVector(expandScores(wikapidiaScoreDocs));
    }

    private QueryBuilder getQueryBuilderByLanguage(Language language) {
        QueryBuilder builder = searcher.getQueryBuilderByLanguage(language);
        WpIdFilter filter = conceptFilter.get(language);
        if (filter != null) {
            builder.addFilter(filter);
        }
        return builder;
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
            return mostSimilar;
        }
        SRResultList srResults = baseMostSimilar(localPage.toLocalId(),maxResults,validIds);
        return normalize(srResults, localPage.getLanguage());
    }

    public String getName() {
        return "ESA";
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException, WikapidiaException {
        PairwiseSimilarity pairwiseSimilarity = new PairwiseCosineSimilarity();
        super.writeCosimilarity(path, languages, maxHits,pairwiseSimilarity);
    }



//    @Override
//    public void writeCosimilarity(String path, LanguageSet languages, final int maxhits) throws IOException, DaoException {
//        final ValueConf vconf = new ValueConf();
//        for (final Language language: languages) {
//            String fullPath = path + getName() + "/matrix/" + language.getLangCode();
//            final SparseMatrixWriter writer = new SparseMatrixWriter(new File(fullPath+"-cosimilarity"), vconf);
//            DaoFilter pageFilter = new DaoFilter().setLanguages(language);
//            Iterable<LocalPage> localPages = pageHelper.get(pageFilter);
//            TIntSet pageIds = new TIntHashSet();
//            for (LocalPage page : localPages) {
//                if (page != null) {
//                    pageIds.add(page.getLocalId());
//                }
//            }
//            List<Integer> wpIds = Arrays.asList(ArrayUtils.toObject(pageIds.toArray()));
//            ParallelForEach.loop(wpIds, new Procedure<Integer>() {
//                public void call(Integer wpId) throws IOException, DaoException {
//                    SRResultList scores = baseMostSimilar(new LocalId(language,wpId),maxhits,null);
//                    if (scores !=null){
//                        int ids[]=scores.getIds();
//                        writer.writeRow(new SparseMatrixRow(vconf, wpId, ids, scores.getScoresAsFloat()));
//                    }
//                }
//            }, Integer.MAX_VALUE);
//            writer.finish();
//            mostSimilarLocalMatrices.put(language,new SparseMatrix(new File(fullPath+"-cosimilarity")));
//        }
//    }

    /**
     * Construct mostSimilar results without normalizing or accessing the cache.
     * If the matrix was not built, this may not find the highest scores,
     * but should find a set that is fairly close.
     * @param localPage
     * @param maxResults
     * @param validIds
     * @return
     * @throws DaoException
     */
    private SRResultList baseMostSimilar(LocalId localPage, int maxResults, TIntSet validIds) throws DaoException {
        Language language = localPage.getLanguage();
        int luceneId = searcher.getDocIdFromLocalId(localPage.getId(), language);
        WikapidiaScoreDoc[] wikapidiaScoreDocs = getQueryBuilderByLanguage(language)
                                    .setMoreLikeThisQuery(luceneId)
                                    .setNumHits(maxResults*POOL_SIZE)
                                    .search();
        SRResultList srResults = new SRResultList(wikapidiaScoreDocs.length);
        int i = 0;
        TIntDoubleHashMap vector = getVector(localPage.getId(),localPage.getLanguage());
        List<SRResult> results = new ArrayList<SRResult>();
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            int localPageId = searcher.getLocalIdFromDocId(wikapidiaScoreDoc.doc, language);
            TIntDoubleHashMap comparison = getVector(localPageId, localPage.getLanguage());
            if (validIds==null||validIds.contains(localPageId)){
                SRResult result = new SRResult(localPageId, SimUtils.cosineSimilarity(vector,comparison));
                results.add(result);
            }
        }
        Collections.sort(results);
        Collections.reverse(results);
        SRResultList resultList = new SRResultList(maxResults);
        for (int j = 0; j < maxResults && j < results.size(); j++){
            resultList.set(j, results.get(j));
        }
        return resultList;
    }

    @Override
    protected synchronized void trainMostSimilarNormalizer(final Dataset dataset, final int numResults, final TIntSet validIds, boolean isDefault) {
        if (resolvePhrases){
            super.trainMostSimilarNormalizer(dataset, numResults, validIds, isDefault);
        }
        final Normalizer trainee;
        if (isDefault){
            trainee = defaultMostSimilarNormalizer;
            defaultMostSimilarNormalizer = new IdentityNormalizer();

        } else {
            trainee = mostSimilarNormalizers.get((int)dataset.getLanguage().getId());
            mostSimilarNormalizers.put((int)dataset.getLanguage().getId(), new IdentityNormalizer());
        }
        ParallelForEach.loop(dataset.getData(), new Procedure<KnownSim>() {
            public void call(KnownSim ks) throws DaoException {
                ks.maybeSwap();
                List<LocalString> localStrings = new ArrayList<LocalString>();
                localStrings.add(new LocalString(ks.language, ks.phrase1));
                localStrings.add(new LocalString(ks.language, ks.phrase2));
                List<LocalId> ids = disambiguator.disambiguate(localStrings, null);
                SRResultList dsl = mostSimilar(localStrings.get(0), numResults, validIds);
                if (dsl != null) {
                    trainee.observe(dsl, dsl.getIndexForId(ids.get(1).getId()), ks.similarity);
                }
            }
        },100);
        trainee.observationsFinished();
        if (isDefault){
            defaultMostSimilarNormalizer = trainee;
        } else {
            mostSimilarNormalizers.put((int)dataset.getLanguage().getId(),trainee);
        }
        LOG.info("trained most similar normalizer for " + getName() + ": " + trainee.dump());
    }

    @Override
    public double[][] cosimilarity(String[] phrases, Language language) throws DaoException {
        TIntDoubleHashMap[] vectors = new TIntDoubleHashMap[phrases.length];
        for (int i=0; i<phrases.length; i++){
            vectors[i]=getVector(phrases[i],language);
        }
        double[][] cos = new double[phrases.length][phrases.length];
        for (int i=0; i<phrases.length; i++){
            cos[i][i]=normalize(1.0,language);
        }
        for (int i=0; i<phrases.length; i++){
            for (int j=i+1; j<phrases.length; j++){
                cos[i][j]= normalize(SimUtils.cosineSimilarity(vectors[i],vectors[j]),language);
                cos[j][i]=cos[i][j];
            }
        }
        return cos;

    }

    @Override
    public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases, Language language) throws DaoException {
        TIntDoubleHashMap[] rowVectors = new TIntDoubleHashMap[rowPhrases.length];
        for (int i=0; i<rowPhrases.length; i++){
            rowVectors[i]=getVector(rowPhrases[i],language);
        }
        TIntDoubleHashMap[] colVectors = new TIntDoubleHashMap[colPhrases.length];
        for (int i=0; i<colPhrases.length; i++){
            colVectors[i]=getVector(colPhrases[i],language);
        }
        double [][] cos = new double[rowPhrases.length][colPhrases.length];
        for (int i=0; i<rowPhrases.length; i++){
            for (int j=0; j<colPhrases.length; j++){
                if (rowPhrases[i].equals(colPhrases[j])){
                    cos[i][j]=normalize(new SRResult(1.0),language).getScore();
                }
                else {
                    cos[i][j]=normalize (SimUtils.cosineSimilarity(rowVectors[i],colVectors[j]),language);
                }
            }
        }
        return cos;

    }

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds, Language language) throws DaoException {
        TIntDoubleHashMap[] rowVectors = new TIntDoubleHashMap[wpRowIds.length];
        for (int i=0; i<wpRowIds.length; i++){
            rowVectors[i]=getVector(wpRowIds[i],language);
        }
        TIntDoubleHashMap[] colVectors = new TIntDoubleHashMap[wpColIds.length];
        for (int i=0; i<wpColIds.length; i++){
            colVectors[i]=getVector(wpColIds[i],language);
        }
        double [][] cos = new double[wpRowIds.length][wpColIds.length];
        for (int i=0; i<wpRowIds.length; i++){
            for (int j=0; j<wpColIds.length; j++){
                if (wpRowIds[i]==wpColIds[j]){
                    cos[i][j]=normalize(new SRResult(1.0),language).getScore();
                }
                else {
                    cos[i][j]=normalize (SimUtils.cosineSimilarity(rowVectors[i],colVectors[j]),language);
                }
            }
        }
        return cos;

    }

    @Override
    public double[][] cosimilarity(int [] ids, Language language) throws DaoException {
        TIntDoubleHashMap[] vectors = new TIntDoubleHashMap[ids.length];
        for (int i=0; i<ids.length; i++){
            vectors[i]=getVector(ids[i],language);
        }
        double[][] cos = new double[ids.length][ids.length];
        for (int i=0; i<ids.length; i++){
            cos[i][i]=normalize(1.0,language);
        }
        for (int i=0; i<ids.length; i++){
            for (int j=i+1; j<ids.length; j++){
                cos[i][j]= normalize(SimUtils.cosineSimilarity(vectors[i],vectors[j]),language);
                cos[j][i]=cos[i][j];
            }
        }
        return cos;
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
                        getConfigurator().get(LocalPageDao.class, config.getString("pageDao")),
                        getConfigurator().get(Disambiguator.class, config.getString("disambiguator")),
                        config.getBoolean("resolvephrases")
            );
            if (config.hasPath("concepts")) {
                try {
                    sr.setConcepts(new File(config.getString("concepts")));
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
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
