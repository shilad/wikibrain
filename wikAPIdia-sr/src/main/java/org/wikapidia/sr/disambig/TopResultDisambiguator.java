package org.wikapidia.sr.disambig;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Resolves disambiguations by naively choosing the most common
 * meaning of a phrase without regards to its context.
 */
public class TopResultDisambiguator implements Disambiguator{
    private final PhraseAnalyzer phraseAnalyzer;

    TopResultDisambiguator(PhraseAnalyzer phraseAnalyzer){
        this.phraseAnalyzer=phraseAnalyzer;
    }


    @Override
    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException {
        Iterator<LocalPage> pageIterator = phraseAnalyzer.resolveLocal(phrase.getLanguage(),phrase.getString(),1).keySet().iterator();
        if (pageIterator.hasNext()){
            LocalPage localPage = pageIterator.next();
            return new LocalId(localPage.getLanguage(),localPage.getLocalId());
        }
        else {
            return null;
        }
    }

    @Override
    public List<LocalId> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LocalId> ids = new ArrayList<LocalId>();
        for (LocalString phrase : phrases){
            ids.add(disambiguate(phrase,context));
        }
        return ids;
    }
}
