package org.wikapidia.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.BaseLocalSRMetric;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.utils.KnownSim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *@author Matt Lesicko
 **/
public class MostSimilarCosineMetric extends BaseLocalSRMetric{
    final double TRAINING_SCORE_CUTOFF = -1;
    final int MAX_RESULTS = 100;
    LocalSRMetric baseMetric;

    public MostSimilarCosineMetric(Disambiguator disambiguator, LocalPageDao pageHelper, LocalSRMetric baseMetric){
        this.disambiguator=disambiguator;
        this.pageHelper=pageHelper;
        this.baseMetric=baseMetric;
    }

    @Override
    public String getName() {
        return "MostSimilarCosine";
    }

    @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) throws DaoException {
        SRResultList mostSimilar1 = baseMetric.mostSimilar(page1,MAX_RESULTS);
        SRResultList mostSimilar2 = baseMetric.mostSimilar(page2,MAX_RESULTS);
        TIntDoubleMap vector1 = new TIntDoubleHashMap();
        double dot = 0;
        double lena = 0;
        double lenb = 0;
        for (SRResult result : mostSimilar1){
            lena+=(result.getScore()*result.getScore());
            vector1.put(result.getId(),result.getScore());
        }
        for (SRResult result: mostSimilar2){
            lenb+=(result.getScore()*result.getScore());
            if (vector1.containsKey(result.getId())){
                dot+=result.getScore()*vector1.get(result.getId());
            }
        }
        return new SRResult(dot/(Math.sqrt(lena)*Math.sqrt(lenb)));
    }

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
        List<KnownSim> highScores = new ArrayList<KnownSim>();
        for (KnownSim ks : dataset.getData()){
            if (ks.similarity > TRAINING_SCORE_CUTOFF){
                highScores.add(ks);
            }
        }
        Dataset highDataset = new Dataset(dataset + "-mostSimilar", dataset.getLanguage(),highScores);
        baseMetric.trainMostSimilar(highDataset,MAX_RESULTS,null);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults) throws DaoException {
        return mostSimilar(page, maxResults,null);
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, TIntSet validIds) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TIntDoubleMap getVector(int id, Language language) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCosimilarity(String path, LanguageSet languages, int maxHits) throws IOException, DaoException, WikapidiaException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readCosimilarity(String path, LanguageSet languages) throws IOException {
        throw new UnsupportedOperationException();
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
            if (!config.getString("type").equals("mostsimilarcosine")) {
                return null;
            }

            MostSimilarCosineMetric sr = new MostSimilarCosineMetric(
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator")),
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(LocalSRMetric.class,config.getString("basemetric"))
            );

            return sr;
        }

    }
}
