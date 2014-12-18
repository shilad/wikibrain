package org.wikibrain.sr.disambig;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.collections.CollectionUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.utils.WbMathUtils;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.*;

public class SimilarityDisambiguator extends Disambiguator {

    public static final int DEFAULT_NUM_CANDIDATES = 5;
    protected final PhraseAnalyzer phraseAnalyzer;
    private int numCandidates = DEFAULT_NUM_CANDIDATES;
    private Language language;

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
    private Criteria criteria = Criteria.SUM;

    private final SRMetric metric;

    public SimilarityDisambiguator(PhraseAnalyzer phraseAnalyzer, SRMetric metric) {
        this.phraseAnalyzer = phraseAnalyzer;
        this.metric = metric;
        this.language = metric.getLanguage();
    }

    @Override
    public List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LinkedHashMap<LocalId, Float>> results = new ArrayList<LinkedHashMap<LocalId, Float>>();

        List<LocalString> allPhrases = new ArrayList<LocalString>(
                (context == null) ? phrases : CollectionUtils.union(phrases, context));

        // Step 0: calculate most frequent candidate senses for each phrase
        Map<LocalString, LinkedHashMap<LocalId, Float>> candidates = Maps.newHashMap();
        for (LocalString s : allPhrases) {
            candidates.put(s, phraseAnalyzer.resolve(s.getLanguage(), s.getString(), numCandidates));
        }

        // Skip using the sr metric at all!
        if (criteria == Criteria.POPULARITY) {
            for (LocalString phrase : phrases) {
                LinkedHashMap<LocalId, Float> m = new LinkedHashMap<LocalId, Float>();
                for (LocalId li : candidates.get(phrase).keySet()) {
                    m.put(li, candidates.get(phrase).get(li));
                }
                results.add(m);
            }
            return results;
        }

        // Step 2: calculate the sum of cosimilarities for each page
        Map<LocalId, Float> pageSims = getCosimilaritySums(candidates);

        // Step 3: multiply background probability by sim sums, choose best product
        List<LinkedHashMap<LocalId, Float>> result = new ArrayList<LinkedHashMap<LocalId, Float>>();
        for (LocalString ls : phrases) {
            Map<LocalId, Float> phraseCands = candidates.get(ls);
            LinkedHashMap<LocalId, Float> pageResult = selectFinalPhraseSenses(pageSims, phraseCands);
            result.add(pageResult);
        }
        return result;
    }

    private LinkedHashMap<LocalId, Float> selectFinalPhraseSenses(Map<LocalId, Float> pageSims, Map<LocalId, Float> phrasePops) {
        if (phrasePops == null || phrasePops.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (LocalId lp : phrasePops.keySet()) {
            float pop = phrasePops.get(lp);
            float sim =  pageSims.get(lp);

            float score;
            switch (criteria) {
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
        for (LocalId key : WpCollectionUtils.sortMapKeys(phrasePops, true)) {
            pageResult.put(key, (float)(phrasePops.get(key) / sum));
        }
        return pageResult;
    }

    /**
     * Return the sum of cosimilarity scores for all unique pages among the candidates.
     * @param candidates
     * @return
     * @throws DaoException
     */
    private Map<LocalId, Float> getCosimilaritySums(Map<LocalString, LinkedHashMap<LocalId, Float>> candidates) throws DaoException {
    	
        // Step 1: compute the page cosimilarity matrix
        Set<LocalId> uniques = new HashSet<LocalId>();
        for (LinkedHashMap<LocalId, Float> prob : candidates.values()) {
            uniques.addAll(prob.keySet());
        }
        List<LocalId> pages = new ArrayList<LocalId>(uniques);
        double[][] cosim;

        if (pages.isEmpty()){
            cosim = new double[0][0];
        } else {
            if (!pages.get(0).getLanguage().equals(language)) {
                throw new DaoException("Expected language " + language + ", found " +  pages.get(0).getLanguage());
            }
            int[] pageIds = new int[pages.size()];
            for (int i=0; i<pages.size(); i++){
                pageIds[i] = pages.get(i).getId();
            }
            cosim = metric.cosimilarity(pageIds);
        }

        // Step 2: calculate the sum of cosimilarities for each page
        Map<LocalId, Float> pageSims = new HashMap<LocalId, Float>();
        for (int i = 0; i < pages.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < pages.size(); j++) {
                if (i != j && WbMathUtils.isReal(cosim[i][j])) {
                    sum += Math.max(0, cosim[i][j]);    // Hack: no negative numbers
                }
            }
            // add 0.0001 to give every candidate a tiny chance and avoid divide by zero errors when there are no good options
            pageSims.put(pages.get(i), (float)(sum + 0.0001));
        }
        return pageSims;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public int getNumCandidates() {
        return numCandidates;
    }

    public void setNumCandidates(int numCandidates) {
        this.numCandidates = numCandidates;
    }

    public static class Provider extends org.wikibrain.conf.Provider<Disambiguator>{
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

            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("SimpleMilneWitten requires 'language' runtime parameter.");
            }
            Language lang = Language.getByLangCode(runtimeParams.get("language"));

            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class, config.getString("phraseAnalyzer"));

            // Create override config for sr metric and load it.
            String srName = config.getString("metric");
            Config newConfig = getConfig().get().getConfig("sr.metric.local." + srName)
                    .withValue("disambiguator", ConfigValueFactory.fromAnyRef("topResult"));
            Map<String, String> srRuntimeParams = new HashMap<String, String>();
            srRuntimeParams.put("language", lang.getLangCode());
            SRMetric sr = getConfigurator().construct(SRMetric.class, srName, newConfig, srRuntimeParams);

            SimilarityDisambiguator dab = new SimilarityDisambiguator(pa, sr);
            if (config.hasPath("criteria")) {
                dab.setCriteria(Criteria.valueOf(config.getString("criteria").toUpperCase()));
            }
            return dab;
        }
    }
}
