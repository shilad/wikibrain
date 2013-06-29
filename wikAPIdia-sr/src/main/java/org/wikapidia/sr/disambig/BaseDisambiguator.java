package org.wikapidia.sr.disambig;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.util.*;

public abstract class BaseDisambiguator implements Disambiguator{
    protected final PhraseAnalyzer phraseAnalyzer;
    protected final LocalSRMetric srMetric;
    int maxResults;

    BaseDisambiguator(PhraseAnalyzer phraseAnalyzer, LocalSRMetric srMetric, int maxResults){
        this.phraseAnalyzer = phraseAnalyzer;
        this.srMetric = srMetric;
        this.maxResults = maxResults;

    }

    @Override
    public List<LocalId> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LocalId> ids = new ArrayList<LocalId>();
        for (LocalString phrase : phrases){
            ids.add(disambiguate(phrase,context));
        }
        return ids;
    }

    protected LocalId topResult(LinkedHashMap<LocalPage,Float> results) throws DaoException{
        Iterator<LocalPage> pageIterator = results.keySet().iterator();
        if (pageIterator.hasNext()){
            LocalPage localPage = pageIterator.next();
            return new LocalId(localPage.getLanguage(),localPage.getLocalId());
        }
        else {
            return null;
        }
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
