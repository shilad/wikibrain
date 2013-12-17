package org.wikapidia.sr.disambig;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.utils.MathUtils;

import java.util.*;

public abstract class BaseDisambiguator implements Disambiguator{
    public static final int DEFAULT_NUM_CANDIDATES = 5;
    protected final PhraseAnalyzer phraseAnalyzer;
    private int numCandidates = DEFAULT_NUM_CANDIDATES;

    BaseDisambiguator(PhraseAnalyzer phraseAnalyzer){
        this.phraseAnalyzer = phraseAnalyzer;
    }

    @Override
    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException {
        return disambiguate(Arrays.asList(phrase), context).get(0);
    }

    @Override
    public List<LocalId> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LocalString> allPhrases = new ArrayList<LocalString>(CollectionUtils.union(phrases, context));

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
                    sum += cosim[i][j];
                }
            }
            pageSums.put(pages.get(i), sum);
        }

        // Step 3: multiply background probability by sim sums, choose best product
        List<LocalId> result = new ArrayList<LocalId>();
        for (LocalString ls : phrases) {
            double bestScore = -1.0;
            LocalPage bestPage = null;

            for (Map.Entry<LocalPage, Float> entry : candidates.get(ls).entrySet()) {
                LocalPage lp = entry.getKey();
                double score = entry.getValue() * pageSums.get(lp);
                if (score > bestScore) {
                    bestScore = score;
                    bestPage = lp;
                }
            }
            result.add((bestPage == null) ? null : bestPage.toLocalId());
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
