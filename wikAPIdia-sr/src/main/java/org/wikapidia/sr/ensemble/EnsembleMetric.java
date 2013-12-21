package org.wikapidia.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.*;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.utils.*;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matt Lesicko
 * @author Shilad Sen
 */
public class EnsembleMetric extends BaseMonolingualSRMetric {
    private static final Logger LOG = Logger.getLogger(EnsembleMetric.class.getName());

    private final int EXTRA_SEARCH_DEPTH = 2;
    private List<MonolingualSRMetric> metrics;
    private Ensemble ensemble;
    private boolean resolvePhrases = true;

    private SparseMatrix mostSimilarMatrices = null;

    public EnsembleMetric(Language language, List<MonolingualSRMetric> metrics, Ensemble ensemble, Disambiguator disambiguator, LocalPageDao pageHelper){
        super(language, pageHelper, disambiguator);
        this.metrics=metrics;
        this.ensemble=ensemble;
    }

    @Override
    public String getName() {
        return "ensemble";
    }

    public List<MonolingualSRMetric> getMetrics() {
        return metrics;
    }

    public void setResolvePhrases(boolean resolvePhrases) {
        this.resolvePhrases = resolvePhrases;
    }

    @Override
    public boolean hasCachedMostSimilarLocal(int wpId){
        if (mostSimilarMatrices == null) {
            return false;
        }
        try {
            return mostSimilarMatrices.getRow(wpId)!=null;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public SRResultList getCachedMostSimilarLocal(int wpId, int numResults, TIntSet validIds){
        if (!hasCachedMostSimilarLocal(wpId)){
            return null;
        }
        SparseMatrixRow row;
        try{
            row = mostSimilarMatrices.getRow(wpId);
        } catch (IOException e){
            return null;
        }
        Leaderboard leaderboard = new Leaderboard(numResults);
        for (int i=0; i<row.getNumCols() ; i++){
            int wpId2 = row.getColIndex(i);
            float value = row.getColValue(i);
            if (validIds == null || validIds.contains(wpId2)){
                leaderboard.tallyScore(wpId2,value);
            }
        }
        SRResultList results = leaderboard.getTop();
        results.sortDescending();
        return results;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (MonolingualSRMetric metric : metrics){
            scores.add(metric.similarity(pageId1,pageId2,explanations));
            }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        if (resolvePhrases) {
            return super.similarity(phrase1,phrase2,explanations);
        }
        List<SRResult> scores = new ArrayList<SRResult>();
        for (MonolingualSRMetric metric : metrics){
            scores.add(metric.similarity(phrase1,phrase2,explanations));
        }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        return mostSimilar(pageId,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        if (hasCachedMostSimilarLocal(pageId)){
            SRResultList mostSimilar= getCachedMostSimilarLocal(pageId, maxResults, validIds);
            if (mostSimilar.numDocs()>maxResults){
                mostSimilar.truncate(maxResults);
            }
            return mostSimilar;
        } else {
            List<SRResultList> scores = new ArrayList<SRResultList>();
            for (MonolingualSRMetric metric : metrics){
                scores.add(metric.mostSimilar(pageId,maxResults*EXTRA_SEARCH_DEPTH,validIds));
            }
            return ensemble.predictMostSimilar(scores,maxResults);
        }
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        if (resolvePhrases) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (MonolingualSRMetric metric : metrics){
            scores.add(metric.mostSimilar(phrase,maxResults*EXTRA_SEARCH_DEPTH,validIds));
        }
        return ensemble.predictMostSimilar(scores,maxResults);
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        for (MonolingualSRMetric metric : metrics) {
            metric.trainSimilarity(dataset);
        }
        List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();

        for (KnownSim ks : dataset.getData()){
            EnsembleSim es = new EnsembleSim(ks);
            for (MonolingualSRMetric metric : metrics){
                double score = Double.NaN;
                try {
                    score = metric.similarity(ks.phrase1,ks.phrase2,false).getScore();
                } catch (Exception e){
                    LOG.log(Level.WARNING, "Local sr metric " + metric.getName() + " failed for " + ks, e);
                }
                es.add(score, 0);
            }
            ensembleSims.add(es);
        }
        ensemble.trainSimilarity(ensembleSims);
        super.trainSimilarity(dataset);
    }

    /**
     * TODO: adapt this to a MostSimilarDataset
     * @param dataset
     * @param numResults
     * @param validIds
     */
    @Override
    public void trainMostSimilar(Dataset dataset, final int numResults, final TIntSet validIds){
        mostSimilarMatrices = null;
        for (MonolingualSRMetric metric : metrics){
            metric.trainMostSimilar(dataset,numResults,validIds);
        }
        List<EnsembleSim> ensembleSims = ParallelForEach.loop(dataset.getData(), new Function<KnownSim, EnsembleSim>() {
            public EnsembleSim call(KnownSim ks) throws DaoException {
                List<LocalString> localStrings = Arrays.asList(
                        new LocalString(ks.language, ks.phrase1),
                        new LocalString(ks.language, ks.phrase2)
                );
                List<LocalId> ids = getDisambiguator().disambiguateTop(localStrings, null);
                if (ids.isEmpty() || ids.get(0).getId() <= 0) {
                    return null;
                }
                int pageId = ids.get(0).getId();
                EnsembleSim es = new EnsembleSim(ks);
                for (MonolingualSRMetric metric : metrics) {
                    double score = Double.NaN;
                    int rank = -1;
                    try {
                        SRResultList dsl = metric.mostSimilar(pageId, numResults * EXTRA_SEARCH_DEPTH, validIds);
                        if (dsl != null && dsl.getIndexForId(ids.get(1).getId()) >= 0) {
                            score = dsl.getScore(dsl.getIndexForId(ids.get(1).getId()));
                            rank = dsl.getIndexForId(ids.get(1).getId());
                        }
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Local sr metric " + metric.getName() + " failed for " + pageId, e);
                    } finally {
                        es.add(score, rank);
                    }
                }
                return es;
            }
        }, 100);
        ensemble.trainMostSimilar(ensembleSims);
        super.trainMostSimilar(dataset, numResults, validIds);
    }

    @Override
    public void write(String path) throws  IOException {
        super.write(path);
        File dir = new File(path);
        ensemble.write(new File(dir, "ensemble").getAbsolutePath());
    }

    @Override
    public void read(String path) throws IOException{
        super.read(path);
        File dir = new File(path);
        ensemble.read(new File(dir, "ensemble").getAbsolutePath());
    }

    @Override
    public TIntDoubleMap getVector(int id) throws DaoException {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, int maxHits, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikapidiaException {
        super.writeCosimilarity(path, maxHits, null, rowIds, colIds);
    }

    @Override
    public void readCosimilarity(String dir) throws IOException {
        super.readCosimilarity(dir, null);
    }

    public static class Provider extends org.wikapidia.conf.Provider<MonolingualSRMetric>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return MonolingualSRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public MonolingualSRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException{
            if (!config.getString("type").equals("ensemble")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            if (!config.hasPath("metrics")){
                throw new ConfigurationException("Ensemble metric has no base metrics to use.");
            }
            EnsembleMetric sr;
            List<MonolingualSRMetric> metrics = new ArrayList<MonolingualSRMetric>();
            for (String metric : config.getStringList("metrics")){
                metrics.add(getConfigurator().get(MonolingualSRMetric.class, metric, "language", language.getLangCode()));
            }
            Ensemble ensemble;
            if (config.getString("ensemble").equals("linear")){
                ensemble = new LinearEnsemble(metrics.size());
            } else if (config.getString("ensemble").equals("even")){
                ensemble = new EvenEnsemble();
            } else {
                throw new ConfigurationException("I don't know how to do that ensemble.");
            }
            Disambiguator disambiguator = getConfigurator().get(Disambiguator.class,config.getString("disambiguator"));
            LocalPageDao pagehelper = getConfigurator().get(LocalPageDao.class,config.getString("pageDao"));
            sr = new EnsembleMetric(language, metrics,ensemble,disambiguator,pagehelper);
            if (config.hasPath("resolvephrases")) {
                sr.resolvePhrases = config.getBoolean("resolvephrases");
            }

            BaseMonolingualSRMetric.configureBase(getConfigurator(), sr, config);
            return sr;
        }
    }
}
