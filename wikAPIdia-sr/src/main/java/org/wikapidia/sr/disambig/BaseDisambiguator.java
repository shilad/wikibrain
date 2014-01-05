package org.wikapidia.sr.disambig;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.utils.MathUtils;
import org.wikapidia.utils.WpCollectionUtils;

import java.util.*;

public abstract class BaseDisambiguator extends Disambiguator{
    public static final int DEFAULT_NUM_CANDIDATES = 5;
    protected final PhraseAnalyzer phraseAnalyzer;
    private int numCandidates = DEFAULT_NUM_CANDIDATES;

    BaseDisambiguator(PhraseAnalyzer phraseAnalyzer){
        this.phraseAnalyzer = phraseAnalyzer;
    }


    @Override
    public List<LinkedHashMap<LocalId, Double>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LocalString> allPhrases = new ArrayList<LocalString>(
                (context == null) ? phrases : CollectionUtils.union(phrases, context));

        // Step 0: calculate most frequent candidate senses for each phrase
        Map<LocalString, LinkedHashMap<LocalPage, Float>> candidates = Maps.newHashMap();
        for (LocalString s : allPhrases) {
            candidates.put(s, phraseAnalyzer.resolveLocal(s.getLanguage(), s.getString(), numCandidates));
        }

        // Step 1: compute the page cosimilarity matrix
        Set<LocalPage> uniques = new HashSet<LocalPage>();
        for (LinkedHashMap<LocalPage, Float> prob : candidates.values()) {
            uniques.addAll(prob.keySet());
        }
        List<LocalPage> pages = new ArrayList<LocalPage>(uniques);
        double[][] cosim = getCosimilarity(pages);

        // Step 2: calculate the sum of cosimilarities for each page
        Map<LocalPage, Double> pageSums = new HashMap<LocalPage, Double>();
        for (int i = 0; i < pages.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < pages.size(); j++) {
                if (i != j && MathUtils.isReal(cosim[i][j])) {
                    double c = cosim[i][j];
                    if (Double.isInfinite(c) && Double.isNaN(c)) {
                        cosim[i][j] = 0.0;      // hack!
                    }
                    sum += cosim[i][j];
                }
            }
            // add 0.0001 to give every candidate a tiny chance and avoid divide by zero errors when there are no good options
            pageSums.put(pages.get(i), sum + 0.0001);
        }

        // Step 3: multiply background probability by sim sums, choose best product
        List<LinkedHashMap<LocalId, Double>> result = new ArrayList<LinkedHashMap<LocalId, Double>>();
        for (LocalString ls : phrases) {
            Map<LocalPage, Float> phraseCands = candidates.get(ls);
            if (phraseCands == null || phraseCands.isEmpty()) {
                result.add(null);
                continue;
            }
            double sum = 0.0;
            for (LocalPage lp : phraseCands.keySet()) {
                float score = (float) (phraseCands.get(lp) * pageSums.get(lp));
                phraseCands.put(lp, score);
                sum += score;
            }
            LinkedHashMap<LocalId, Double> pageResult = new LinkedHashMap<LocalId, Double>();
            for (LocalPage key : WpCollectionUtils.sortMapKeys(phraseCands, true)) {
                pageResult.put(key.toLocalId(), phraseCands.get(key) / sum);
            }
            result.add(pageResult);
        }
        return result;
    }

    protected abstract double[][] getCosimilarity(List<LocalPage> pages) throws DaoException;

    public int getNumCandidates() {
        return numCandidates;
    }

    public void setNumCandidates(int numCandidates) {
        this.numCandidates = numCandidates;
    }
}
