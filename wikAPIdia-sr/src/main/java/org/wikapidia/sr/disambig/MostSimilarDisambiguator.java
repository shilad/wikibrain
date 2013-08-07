package org.wikapidia.sr.disambig;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;


//Currently unsure what to do about this class.
//Do we ever want to use a Most Similar Disambiguator?
public class MostSimilarDisambiguator extends BaseDisambiguator{
    MostSimilarDisambiguator(PhraseAnalyzer phraseAnalyzer, LocalSRMetric srMetric) {
        super(phraseAnalyzer, srMetric);
    }

    @Override
    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException {
        LocalId result = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        LinkedHashMap<LocalPage,Float> candidates1 = phraseAnalyzer.resolveLocal(phrase.getLanguage(),phrase.getString(),getNumCandidates());

        //If nothing can be done, just return null
        if (candidates1==null||candidates1.isEmpty()){
            return null;
        }

        //If there is no context, just return the top match
        if (context==null||context.isEmpty()){
            Iterator<LocalPage> iterator = candidates1.keySet().iterator();
            if (iterator.hasNext()){
                return iterator.next().toLocalId();
            }
            else{
                return null;
            }
        }
        for (LocalString contextString: context){
            LinkedHashMap<LocalPage,Float> candidates2 = phraseAnalyzer.resolveLocal(contextString.getLanguage(),contextString.getString(),getNumCandidates());
            if (candidates2==null||candidates2.isEmpty()){
                continue;
            }
            for (LocalPage candidate1 : candidates1.keySet()){
                double score1 = candidates1.get(candidate1);
                SRResultList mostSimilar = srMetric.mostSimilar(candidate1,getNumCandidates());
                for (LocalPage candidate2 : candidates2.keySet()){
                    double score2 = candidates2.get(candidate2);
                    double srscore = 0;
                    for (SRResult score : mostSimilar){
                        if (score.getId()==candidate2.getLocalId()){
                            srscore=score.getScore();
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

    @Override
    protected double[][] getCosimilarity(List<LocalPage> pages) throws DaoException {
        throw new UnsupportedOperationException();
    }
}
