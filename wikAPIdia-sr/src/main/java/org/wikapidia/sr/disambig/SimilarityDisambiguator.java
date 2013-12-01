package org.wikapidia.sr.disambig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.Provider;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.LocalSRMetric;

import java.util.HashMap;
import java.util.List;

public class SimilarityDisambiguator extends BaseDisambiguator{
    SimilarityDisambiguator(PhraseAnalyzer phraseAnalyzer, LocalSRMetric srMetric) {
        super(phraseAnalyzer, srMetric);
    }

    /**
     * Returns the cosimilarity matrix for the specified pages
     * @param pages
     * @return
     * @throws DaoException
     */
    @Override
    protected double[][] getCosimilarity(List<LocalPage> pages) throws DaoException {
        if (pages==null||pages.isEmpty()){
            throw new DaoException();
        }
        Language language = pages.get(0).getLanguage();
        int[] pageIds = new int[pages.size()];
        for (int i=0; i<pages.size(); i++){
            pageIds[i] = pages.get(i).getLocalId();
        }
        return srMetric.cosimilarity(pageIds, language);
    }

    public static class Provider extends org.wikapidia.conf.Provider<Disambiguator>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator,config);
        }

        @Override
        public Class getType(){
            return Disambiguator.class;
        }

        @Override
        public String getPath(){
            return "sr.disambig";
        }

        @Override
        public Disambiguator get(String name, Config config) throws ConfigurationException{
            if (!config.getString("type").equals("similarity")){
                return null;
            }

            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class,config.getString("phraseAnalyzer"));
            HashMap<String, String> map = new HashMap<String,String>();
            String srName = config.getString("metric");
            map.put("disambiguator","topResult");
            Config newConfig = getConfig().get().getConfig("sr.metric.local." + srName).withValue("disambiguator",ConfigValueFactory.fromAnyRef("topResult"));
            LocalSRMetric sr = getConfigurator().construct(LocalSRMetric.class,srName,newConfig);


            return new SimilarityDisambiguator(pa,sr);
        }
    }
}
