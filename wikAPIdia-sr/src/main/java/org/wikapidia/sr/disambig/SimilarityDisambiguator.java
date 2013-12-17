package org.wikapidia.sr.disambig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.MonolingualSRMetric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimilarityDisambiguator extends BaseDisambiguator{
    private final Map<Language, MonolingualSRMetric> metrics;
    SimilarityDisambiguator(PhraseAnalyzer phraseAnalyzer, Map<Language, MonolingualSRMetric> metrics) {
        super(phraseAnalyzer);
        this.metrics = metrics;
    }

    /**
     * Returns the cosimilarity matrix for the specified pages
     * @param pages
     * @return
     * @throws DaoException
     */
    @Override
    protected double[][] getCosimilarity(List<LocalPage> pages) throws DaoException {
        if (pages==null || pages.isEmpty()){
            throw new DaoException();
        }
        Language language = pages.get(0).getLanguage();
        if (!metrics.containsKey(language)) {
            throw new DaoException("No metric for language " + language);
        }
        int[] pageIds = new int[pages.size()];
        for (int i=0; i<pages.size(); i++){
            pageIds[i] = pages.get(i).getLocalId();
        }
        return metrics.get(language).cosimilarity(pageIds);
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
        public Disambiguator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException{
            if (!config.getString("type").equals("similarity")){
                return null;
            }

            LanguageSet langs = getConfigurator().get(LanguageSet.class);
            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class,config.getString("phraseAnalyzer"));

            // Create override config for metric.
            HashMap<String, String> map = new HashMap<String,String>();
            String srName = config.getString("metric");
            map.put("disambiguator","topResult");
            Config newConfig = getConfig().get().getConfig("sr.metric.local." + srName).withValue("disambiguator", ConfigValueFactory.fromAnyRef("topResult"));

            // Load all metrics
            Map<Language, MonolingualSRMetric> metrics = new HashMap<Language, MonolingualSRMetric>();
            for (Language lang : langs) {
                Map<String, String> srRuntimeParams = new HashMap<String, String>();
                srRuntimeParams.put("language", lang.getLangCode());
                MonolingualSRMetric sr = getConfigurator().construct(MonolingualSRMetric.class, srName, newConfig, srRuntimeParams);
                metrics.put(lang, sr);
            }

            return new SimilarityDisambiguator(pa, metrics);
        }
    }
}
