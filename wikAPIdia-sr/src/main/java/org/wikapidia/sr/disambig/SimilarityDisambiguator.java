package org.wikapidia.sr.disambig;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.collections.CollectionUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.utils.MathUtils;
import org.wikapidia.utils.WpCollectionUtils;

import java.util.*;

public class SimilarityDisambiguator extends Disambiguator {

    public static final int DEFAULT_NUM_CANDIDATES = 5;
    protected final PhraseAnalyzer phraseAnalyzer;
    private int numCandidates = DEFAULT_NUM_CANDIDATES;

    private final Map<Language, MonolingualSRMetric> metrics;

    /**
     * Algorithms for disambiguating similar phrases
     */
    public static enum Criteria {
        SUM,         // select senses with highest sum of popularity + similarity
        PRODUCT,     // select senses with highest sum of popularity * similarity
        POPULARITY,  // select most popular senses
        SIMILARITY   // select most similar senses
    }

    // Method for disambiguating similar phrases
    private Criteria critera = Criteria.SUM;

    public SimilarityDisambiguator(PhraseAnalyzer phraseAnalyzer, Map<Language, MonolingualSRMetric> metrics) {
        this.phraseAnalyzer = phraseAnalyzer;
        this.metrics = metrics;
    }

    @Override
    public List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LinkedHashMap<LocalId, Float>> results = new ArrayList<LinkedHashMap<LocalId, Float>>();

        List<LocalString> allPhrases = new ArrayList<LocalString>(
                (context == null) ? phrases : CollectionUtils.union(phrases, context));

        // Step 0: calculate most frequent candidate senses for each phrase
        Map<LocalString, LinkedHashMap<LocalPage, Float>> candidates = Maps.newHashMap();
        for (LocalString s : allPhrases) {
            candidates.put(s, phraseAnalyzer.resolveLocal(s.getLanguage(), s.getString(), numCandidates));
        }

        // Skip using the sr metric at all!
        if (critera == Criteria.POPULARITY) {
            for (LocalString phrase : phrases) {
                LinkedHashMap<LocalId, Float> m = new LinkedHashMap<LocalId, Float>();
                for (LocalPage lp : candidates.get(phrase).keySet()) {
                    m.put(lp.toLocalId(), candidates.get(phrase).get(lp));
                }
                results.add(m);
            }
            return results;
        }

        // Step 2: calculate the sum of cosimilarities for each page
        Map<LocalPage, Float> pageSims = getCosimilaritySums(candidates);

        // Step 3: multiply background probability by sim sums, choose best product
        List<LinkedHashMap<LocalId, Float>> result = new ArrayList<LinkedHashMap<LocalId, Float>>();
        for (LocalString ls : phrases) {
            Map<LocalPage, Float> phraseCands = candidates.get(ls);
            LinkedHashMap<LocalId, Float> pageResult = selectFinalPhraseSenses(pageSims, phraseCands);
            result.add(pageResult);
        }
        return result;
    }

    private LinkedHashMap<LocalId, Float> selectFinalPhraseSenses(Map<LocalPage, Float> pageSims, Map<LocalPage, Float> phrasePops) {
        if (phrasePops == null || phrasePops.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (LocalPage lp : phrasePops.keySet()) {
            float pop = phrasePops.get(lp);
            float sim =  pageSims.get(lp);

            float score;
            switch (critera) {
                case POPULARITY:
                    score = pop;
                    break;
                case SIMILARITY:
                    score = sim;
                    break;
                case SUM:
                    score = pop + sim;
                    break;
                case PRODUCT:
                    score = pop * sim;
                    break;
                default:
                    throw new IllegalStateException();
            }

            phrasePops.put(lp, score);
            sum += score;
        }
        LinkedHashMap<LocalId, Float> pageResult = new LinkedHashMap<LocalId, Float>();
        for (LocalPage key : WpCollectionUtils.sortMapKeys(phrasePops, true)) {
            pageResult.put(key.toLocalId(), (float)(phrasePops.get(key) / sum));
        }
        return pageResult;
    }

    /**
     * Return the sum of cosimilarity scores for all unique pages among the candidates.
     * @param candidates
     * @return
     * @throws DaoException
     */
    private Map<LocalPage, Float> getCosimilaritySums(Map<LocalString, LinkedHashMap<LocalPage, Float>> candidates) throws DaoException {

        // Step 1: compute the page cosimilarity matrix
        Set<LocalPage> uniques = new HashSet<LocalPage>();
        for (LinkedHashMap<LocalPage, Float> prob : candidates.values()) {
            uniques.addAll(prob.keySet());
        }
        List<LocalPage> pages = new ArrayList<LocalPage>(uniques);
        double[][] cosim;

        if (pages.isEmpty()){
            cosim = new double[0][0];
        } else {
            Language language = pages.get(0).getLanguage();
            if (!metrics.containsKey(language)) {
                throw new DaoException("No metric for language " + language);
            }
            int[] pageIds = new int[pages.size()];
            for (int i=0; i<pages.size(); i++){
                pageIds[i] = pages.get(i).getLocalId();
            }
            cosim = metrics.get(language).cosimilarity(pageIds);
        }

        // Step 2: calculate the sum of cosimilarities for each page
        Map<LocalPage, Float> pageSims = new HashMap<LocalPage, Float>();
        for (int i = 0; i < pages.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < pages.size(); j++) {
                if (i != j && MathUtils.isReal(cosim[i][j])) {
                    sum += Math.max(0, cosim[i][j]);    // Hack: no negative numbers
                }
            }
            // add 0.0001 to give every candidate a tiny chance and avoid divide by zero errors when there are no good options
            pageSims.put(pages.get(i), (float)(sum + 0.0001));
        }
        return pageSims;
    }

    public Criteria getCritera() {
        return critera;
    }

    public void setCritera(Criteria critera) {
        this.critera = critera;
    }

    public int getNumCandidates() {
        return numCandidates;
    }

    public void setNumCandidates(int numCandidates) {
        this.numCandidates = numCandidates;
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
            String srName = config.getString("metric");
            Config newConfig = getConfig().get().getConfig("sr.metric.local." + srName)
                    .withValue("disambiguator", ConfigValueFactory.fromAnyRef("topResult"));

            // Load all metrics
            Map<Language, MonolingualSRMetric> metrics = new HashMap<Language, MonolingualSRMetric>();
            for (Language lang : langs) {
                Map<String, String> srRuntimeParams = new HashMap<String, String>();
                srRuntimeParams.put("language", lang.getLangCode());
                MonolingualSRMetric sr = getConfigurator().construct(MonolingualSRMetric.class, srName, newConfig, srRuntimeParams);
                metrics.put(lang, sr);
            }

            SimilarityDisambiguator dab = new SimilarityDisambiguator(pa, metrics);
            if (config.hasPath("criteria")) {
                dab.setCritera(Criteria.valueOf(config.getString("criteria").toUpperCase()));
            }
            return dab;
        }
    }
}
