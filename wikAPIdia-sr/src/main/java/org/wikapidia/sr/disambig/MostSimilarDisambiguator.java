package org.wikapidia.sr.disambig;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;

import java.util.LinkedHashMap;
import java.util.Set;

public class MostSimilarDisambiguator extends BaseDisambiguator{
    MostSimilarDisambiguator(PhraseAnalyzer phraseAnalyzer, LocalSRMetric srMetric, int maxResults) {
        super(phraseAnalyzer, srMetric, maxResults);
    }

    @Override
    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException {
        LocalId result = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        LinkedHashMap<LocalPage,Float> candidates1 = phraseAnalyzer.resolveLocal(phrase.getLanguage(),phrase.getString(),maxResults);

        //If nothing can be done, just return null
        if (candidates1==null||candidates1.isEmpty()){
            return null;
        }

        //If there is no context, just return the top match
        if (context==null||context.isEmpty()){
            return topResult(candidates1);
        }
        for (LocalString contextString: context){
            LinkedHashMap<LocalPage,Float> candidates2 = phraseAnalyzer.resolveLocal(contextString.getLanguage(),contextString.getString(),maxResults);
            if (candidates2==null||candidates2.isEmpty()){
                continue;
            }
            for (LocalPage candidate1 : candidates1.keySet()){
                double score1 = candidates1.get(candidate1);
                SRResultList mostSimilar = srMetric.mostSimilar(candidate1,maxResults,false);
                for (LocalPage candidate2 : candidates2.keySet()){
                    double score2 = candidates2.get(candidate2);
                    double srscore = 0;
                    for (SRResult score : mostSimilar){
                        if (score.getId()==candidate2.getLocalId()){
                            srscore=score.getValue();
                        }
                    }
                    double score = score1*score2*srscore;
                    if (result==null||score>bestScore){
                        result = new LocalId(candidate1.getLanguage(),candidate1.getLocalId());
                        bestScore=score;
                    }
                }
            }
        }
        return result;
    }
}
