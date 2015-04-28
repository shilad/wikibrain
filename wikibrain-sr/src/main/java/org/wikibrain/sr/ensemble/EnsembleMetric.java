package org.wikibrain.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.sr.*;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.KnownSim;
import org.wikibrain.utils.*;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matt Lesicko
 * @author Shilad Sen
 */
public class EnsembleMetric extends BaseSRMetric {
    private static final Logger LOG = LoggerFactory.getLogger(EnsembleMetric.class);

    public static final int MIN_SEARCH_DEPTH = 500;
    public static final int SEARCH_MULTIPLIER = 3;

    private List<SRMetric> metrics;
    private Ensemble ensemble;
    private boolean resolvePhrases = true;
    private boolean trainSubmetrics = true;


    public EnsembleMetric(String name, Language language, List<SRMetric> metrics, Ensemble ensemble, Disambiguator disambiguator, LocalPageDao pageHelper){
        super(name, language, pageHelper, disambiguator);
        this.metrics=metrics;
        this.ensemble=ensemble;
    }

    public List<SRMetric> getMetrics() {
        return metrics;
    }

    public void setResolvePhrases(boolean resolvePhrases) {
        this.resolvePhrases = resolvePhrases;
    }

    @Override
    public SRConfig getConfig() {
        return new SRConfig();
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (SRMetric metric : metrics){
            scores.add(metric.similarity(pageId1,pageId2,explanations));
        }
        return normalize(ensemble.predictSimilarity(scores));
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        if (resolvePhrases) {
            return super.similarity(phrase1, phrase2, explanations);
        }
        List<SRResult> scores = new ArrayList<SRResult>();
        for (SRMetric metric : metrics){
            scores.add(metric.similarity(phrase1,phrase2,explanations));
        }
        return normalize(ensemble.predictSimilarity(scores));
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        SRResultList mostSimilar= getCachedMostSimilar(pageId, maxResults, validIds);
        if (mostSimilar != null) {
            return mostSimilar;
        }
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (SRMetric metric : metrics){
            scores.add(metric.mostSimilar(pageId,getMaxResults(maxResults),validIds));
        }
        SRResultList result = normalize(ensemble.predictMostSimilar(scores, maxResults, validIds));
        return result;
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        if (resolvePhrases) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (SRMetric metric : metrics){
            scores.add(metric.mostSimilar(phrase, getMaxResults(maxResults),validIds));
        }
        return normalize(ensemble.predictMostSimilar(scores,maxResults, validIds));
    }

    /**
     * Training cascades to base metrics.
     * @param dataset
     * @throws DaoException
     */
    @Override
    public void trainSimilarity(final Dataset dataset) throws DaoException {
        if (trainSubmetrics) {
            for (SRMetric metric : metrics) {
                metric.trainSimilarity(dataset);
            }
        }
        final List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();
        ParallelForEach.loop(
                dataset.getData(),
                new Procedure<KnownSim>() {
                    @Override
                    public void call(KnownSim ks) throws Exception {
                        EnsembleSim es = new EnsembleSim(ks);
                        for (SRMetric metric : metrics){
                            double score = Double.NaN;
                            try {
                                SRResult result = metric.similarity(ks.phrase1,ks.phrase2,false);
                                if (result != null) {
                                    score = result.getScore();
                                }
                            } catch (Exception e){
                                LOG.warn("Local sr metric " + metric.getName() + " failed for " + ks, e);
                            }
                            es.add(score, 0);
                        }
                        ensembleSims.add(es);
                    }
                },
                100);
        ensemble.trainSimilarity(ensembleSims);
        super.trainSimilarity(dataset);
    }

    /**
     * Training cascades to base metrics.
     * TODO: adapt this to a MostSimilarDataset
     * @param dataset
     * @param numResults
     * @param validIds
     */
    @Override
    public void trainMostSimilar(Dataset dataset, final int numResults, final TIntSet validIds){
        if (getMostSimilarCache() != null) {
            clearMostSimilarCache();
        }
        if (trainSubmetrics) {
            for (SRMetric metric : metrics){
                metric.trainMostSimilar(dataset,numResults,validIds);
            }
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
                for (SRMetric metric : metrics) {
                    double score = Double.NaN;
                    int rank = -1;
                    try {
                        SRResultList dsl = metric.mostSimilar(pageId, getMaxResults(numResults), validIds);
                        if (dsl != null && dsl.getIndexForId(ids.get(1).getId()) >= 0) {
                            score = dsl.getScore(dsl.getIndexForId(ids.get(1).getId()));
                            rank = dsl.getIndexForId(ids.get(1).getId());
                        }
                    } catch (Exception e) {
                        LOG.warn("Local sr metric " + metric.getName() + " failed for " + pageId, e);
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

    private int getMaxResults(int numResults) {
        return Math.max(MIN_SEARCH_DEPTH, numResults * SEARCH_MULTIPLIER);
    }


    public void setTrainSubmetrics(boolean trainSubmetrics) {
        this.trainSubmetrics = trainSubmetrics;
    }

    @Override
    public void write() throws  IOException {
        super.write();
        ensemble.write(new File(getDataDir(), "ensemble").getAbsolutePath());
    }

    @Override
    public void read() throws IOException{
        super.read();
        ensemble.read(new File(getDataDir(), "ensemble").getAbsolutePath());
    }

    public static class Provider extends org.wikibrain.conf.Provider<SRMetric>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException{
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
            List<SRMetric> metrics = new ArrayList<SRMetric>();
            for (String metric : config.getStringList("metrics")){
                metrics.add(getConfigurator().get(SRMetric.class, metric, "language", language.getLangCode()));
            }
            LocalPageDao pageDao = getConfigurator().get(LocalPageDao.class,config.getString("pageDao"));
            int numArticles = 0;
            try {
                numArticles = pageDao.getCount(DaoFilter.normalPageFilter(language));
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
            Ensemble ensemble;
            if (config.getString("ensemble").equals("linear")){
                ensemble = new CorrelationEnsemble(metrics.size(), numArticles);
            } else if (config.getString("ensemble").equals("even")){
                ensemble = new EvenEnsemble();
            } else {
                throw new ConfigurationException("I don't know how to do that ensemble.");
            }
            Disambiguator disambiguator = getConfigurator().get(Disambiguator.class,config.getString("disambiguator"), "language", language.getLangCode());
            EnsembleMetric sr = new EnsembleMetric(name, language, metrics,ensemble,disambiguator,pageDao);
            if (config.hasPath("resolvephrases")) {
                sr.setResolvePhrases(config.getBoolean("resolvephrases"));
            }

            BaseSRMetric.configureBase(getConfigurator(), sr, config);
            return sr;
        }
    }
}
