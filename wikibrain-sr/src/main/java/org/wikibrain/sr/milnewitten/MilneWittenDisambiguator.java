package org.wikibrain.sr.milnewitten;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.collections.CollectionUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.utils.WbMathUtils;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.*;

/**
 * @author Shilad Sen
 *
 * A literal implementation of the disambiguator described in
 * http://www.aaai.org/Papers/Workshops/2008/WS-08-15/WS08-15-005.pdf
 *
 * This is naively extended to support multiple strings and contexts.
 */
public class MilneWittenDisambiguator extends Disambiguator {
    private final Language language;
    private final LocalPageDao pageDao;
    private final PhraseAnalyzer analyzer;
    private final SRMetric metric;
    private final int numPages;

    /**
     * Construct a new disambiguator that uses a particular metric.
     *
     * @param pageDao
     * @param analyzer
     * @param metric
     * @throws DaoException
     */
    public MilneWittenDisambiguator(LocalPageDao pageDao, PhraseAnalyzer analyzer, SRMetric metric) throws DaoException {
        this.language = metric.getLanguage();
        this.pageDao = pageDao;
        this.analyzer = analyzer;
        this.metric = metric;
        this.numPages = pageDao.getCount(
                new DaoFilter().setLanguages(language)
                        .setNameSpaces(NameSpace.ARTICLE)
                        .setRedirect(false)
                        .setDisambig(false));
    }

    /**
     * Given
     * @param phrases   The target phrases being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return
     * @throws DaoException
     */
    @Override
    public List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {

        // Collect all phrases
        List<LocalString> allPhrases = new ArrayList<LocalString>(
                (context == null) ? phrases : CollectionUtils.union(phrases, context));

        // Step 1: calculate most frequent candidate senses for each phrase
        Map<LocalString, LinkedHashMap<LocalId, Float>> candidates = Maps.newHashMap();
        for (LocalString s : allPhrases) {
            if (!s.getLanguage().equals(language)) {
                throw new IllegalArgumentException("Disambiguator only supports language " + language);
            }
            candidates.put(s, analyzer.resolve(s.getLanguage(), s.getString(), 100));
        }

        // Step 1.5: Build mapping from local id to phrases that are candidates
        Map<LocalId, Set<LocalString>> idsToPhrases = new HashMap<LocalId, Set<LocalString>>();
        for (LocalString s : candidates.keySet()) {
            for (LocalId lid : candidates.get(s).keySet()) {
                if (!idsToPhrases.containsKey(lid)) {
                    idsToPhrases.put(lid, new HashSet<LocalString>());
                }
                idsToPhrases.get(lid).add(s);
            }
        }

        // Step 2: calculate the sum of cosimilarities for each page
        Map<LocalId, Float> pageSims = getCosimilaritySums(candidates);

        // Step 3: Choose the best options for each phrase
        List<LinkedHashMap<LocalId, Float>> result = new ArrayList<LinkedHashMap<LocalId, Float>>();
        for (LocalString p : phrases) {
            result.add(
                    disambiguateOnePhrase(p, candidates.get(p), idsToPhrases, pageSims));
        }
        return result;
    }

    private LinkedHashMap<LocalId, Float> disambiguateOnePhrase(LocalString phrase, LinkedHashMap<LocalId, Float> candidates, Map<LocalId, Set<LocalString>> idsToPhrases, Map<LocalId, Float> pageSims) throws DaoException {
        // Identify the highest similarity for each page
        float maxSimilarity = Float.NEGATIVE_INFINITY;
        for (LocalId lid : candidates.keySet()) {
            maxSimilarity = Math.max(maxSimilarity, pageSims.get(lid));
        }

        // Identify the most popular senses within 40% of the top similarity
        Map<LocalId, Float> scores = new HashMap<LocalId, Float>();
        double scoreSum = 0.0;
        for (LocalId lid : candidates.keySet()) {
            double sim = pageSims.get(lid);
            if (sim < 0.4 * maxSimilarity) {
                continue;
            }
            double pop = candidates.get(lid);

            double phraseBonus = 0.0;
            /*

            TODO: figure out phraseBonus for multiple phrases

            int numPhrases = 0;
            int sumCounts = 0;
            for (LocalString ls2 : idsToPhrases.get(lid)) {
                numPhrases++;
                sumCounts += getPhraseCount(phrase.getString() + " " + ls2.getString());
                sumCounts += getPhraseCount(ls2.getString() + " " + phrase.getString());
            }

            if (sumCounts > 0) {
                int maxExpectedValue = numPhrases * numPages / 50;
                phraseBonus = Math.log(sumCounts + 1) / Math.log(maxExpectedValue);
                phraseBonus = Math.min(0.5, phraseBonus);
            }

            System.err.println("phrase bonus for " + phrase + ", " + idsToPhrases.get(lid) + " is " + phraseBonus);
                         */

            double score = pop + phraseBonus;
            scores.put(lid, (float) score);
            scoreSum += score;

        }
        LinkedHashMap<LocalId, Float> pageResult = new LinkedHashMap<LocalId, Float>();
        for (LocalId key : WpCollectionUtils.sortMapKeys(scores, true)) {
            pageResult.put(key, (float) (scores.get(key) / scoreSum));
        }
        return pageResult;
    }

    /*
    private int getPhraseCount(String phrase) throws DaoException {
        PrunedCounts<Integer> pages = analyzer.getDao().getPhraseCounts(language, phrase, 1);
        if (pages == null) {
            return 0;
        } else {
            return pages.getTotal();
        }
    }
    */


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
            if (!config.getString("type").equals("milnewitten")){
                return null;
            }

            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("SimpleMilneWitten requires 'language' runtime parameter.");
            }
            Language lang = Language.getByLangCode(runtimeParams.get("language"));
            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class, config.getString("phraseAnalyzer"));
            LocalPageDao pageDao = getConfigurator().get(LocalPageDao.class);

            // Create override config for sr metric and load it.
            String srName = config.getString("metric");
            Config newConfig = getConfig().get().getConfig("sr.metric.local." + srName)
                    .withValue("disambiguator", ConfigValueFactory.fromAnyRef("topResult"));
            Map<String, String> srRuntimeParams = new HashMap<String, String>();
            srRuntimeParams.put("language", lang.getLangCode());
            SRMetric sr = getConfigurator().construct(SRMetric.class, srName, newConfig, srRuntimeParams);

            try {
                return new MilneWittenDisambiguator(pageDao, pa, sr);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
