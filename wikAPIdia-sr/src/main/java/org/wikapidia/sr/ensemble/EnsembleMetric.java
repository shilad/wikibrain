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
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.Dataset;
import org.wikapidia.sr.utils.KnownSim;
import org.wikapidia.utils.Function;
import org.wikapidia.utils.ParallelForEach;

import java.io.*;
import java.util.*;

/**
 * @author Matt Lesicko
 */
public class EnsembleMetric extends BaseLocalSRMetric{
    private final double missingScore = 0.0;
    private List<LocalSRMetric> metrics;
    Ensemble ensemble;

    public EnsembleMetric(List<LocalSRMetric> metrics, Ensemble ensemble, Disambiguator disambiguator, LocalPageDao pageHelper){
        this.metrics=metrics;
        this.ensemble=ensemble;
        this.disambiguator=disambiguator;
        this.pageHelper=pageHelper;
    }

    @Override
    public String getName() {
        return "Ensemble";
    }

    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.similarity(page1,page2,explanations));
        }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        List<SRResult> scores = new ArrayList<SRResult>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.similarity(phrase1,phrase2,language,explanations));
        }
        return ensemble.predictSimilarity(scores);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        return mostSimilar(page,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.mostSimilar(page,maxResults,validIds));
        }
        return ensemble.predictMostSimilar(scores,maxResults);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws  DaoException{
        return mostSimilar(phrase,maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException {
        List<SRResultList> scores = new ArrayList<SRResultList>();
        for (LocalSRMetric metric : metrics){
            scores.add(metric.mostSimilar(phrase,maxResults,validIds));
        }
        return ensemble.predictMostSimilar(scores,maxResults);
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();
        for (KnownSim ks : dataset.getData()){
            List<Double> scores = new ArrayList<Double>();
            for (LocalSRMetric metric : metrics){
                double score = metric.similarity(ks.phrase1,ks.phrase2,ks.language,false).getScore();
                if (!Double.isNaN(score)&&!Double.isInfinite(score)){
                    scores.add(score);
                } else {
                    scores.add(0.0);
                }
            }
            ensembleSims.add(new EnsembleSim(scores,ks));
        }
        ensemble.trainSimilarity(ensembleSims);
    }

    @Override
    public void trainDefaultSimilarity(Dataset dataset) throws DaoException {
        List<EnsembleSim> ensembleSims = new ArrayList<EnsembleSim>();
        for (KnownSim ks : dataset.getData()){
            List<Double> scores = new ArrayList<Double>();
            for (LocalSRMetric metric : metrics){
                scores.add(metric.similarity(ks.phrase1,ks.phrase2,ks.language,false).getScore());
            }
            ensembleSims.add(new EnsembleSim(scores,ks));
        }
        ensemble.trainSimilarity(ensembleSims);
    }

    @Override
    public void trainMostSimilar(Dataset dataset, final int numResults, final TIntSet validIds){
        for (LocalSRMetric metric : metrics){
            metric.trainMostSimilar(dataset,numResults,validIds);
        }
        List<EnsembleSim> ensembleSims = ParallelForEach.loop(dataset.getData(), new Function<KnownSim,EnsembleSim>() {
            public EnsembleSim call(KnownSim ks) throws DaoException{
                List<Double> scores = new ArrayList<Double>();
                List<LocalString> localStrings = new ArrayList<LocalString>();
                localStrings.add(new LocalString(ks.language, ks.phrase1));
                localStrings.add(new LocalString(ks.language, ks.phrase2));
                List<LocalId> ids = disambiguator.disambiguate(localStrings, null);
                LocalPage page = pageHelper.getById(ks.language,ids.get(0).getId());
                if (page!=null){
                    for (LocalSRMetric metric : metrics){
                        try {
                            SRResultList dsl = metric.mostSimilar(page, numResults, validIds);
                            if (dsl!=null&&dsl.getIndexForId(ids.get(1).getId())>1){
                                scores.add(dsl.getScore(dsl.getIndexForId(ids.get(1).getId())));
                            }
                            else {
                                scores.add(missingScore);
                            }
                        } catch (DaoException e){scores.add(missingScore);} //In event of metric failure
                    }
                    return new EnsembleSim(scores,ks);
                }
                return null;
            }
        },1);
        ensemble.trainMostSimilar(ensembleSims);
    }

    @Override
    public void write(String path) throws  IOException{
        ensemble.write(path);
    }

    @Override
    public void read(String path) throws IOException{
        ensemble.read(path);
    }

    @Override
    public void trainDefaultMostSimilar(Dataset dataset, int numResults, TIntSet validIds){
        //TODO: implement me
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        //TODO: implement me
        throw new UnsupportedOperationException();
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalSRMetric>{
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
        public LocalSRMetric get(String name, Config config) throws ConfigurationException{
            if (!config.getString("type").equals("Ensemble")) {
                return null;
            }

            List<String> langCodes = getConfig().get().getStringList("languages");

            if (config.hasPath("metrics")){
                EnsembleMetric sr;
                List<LocalSRMetric> metrics = new ArrayList<LocalSRMetric>();
                for (String metric : config.getStringList("metrics")){
                    metrics.add(getConfigurator().get(LocalSRMetric.class,metric));
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
                sr = new EnsembleMetric(metrics,ensemble,disambiguator,pagehelper);

                try {
                    sr.read(getConfigurator().getConf().get().getString("sr.metric.path")+sr.getName());
                } catch (IOException e){
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }


                //Set up normalizers
                sr.setDefaultSimilarityNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                sr.setDefaultMostSimilarNormalizer(getConfigurator().get(Normalizer.class,config.getString("similaritynormalizer")));
                for (String langCode : langCodes){
                    Language language = Language.getByLangCode(langCode);
                    sr.setSimilarityNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                    sr.setMostSimilarNormalizer(getConfigurator().get(Normalizer.class, config.getString("similaritynormalizer")), language);
                }



                return sr;
            }
            else {
                throw new ConfigurationException("Ensemble metric has no base metrics to use.");
            }
        }
    }
}
