package org.wikapidia.sr.disambig;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Matt Lesicko
 */
public class TopResultConsensusDisambiguator implements Disambiguator {
    private List<PhraseAnalyzer> phraseAnalyzers;

    public TopResultConsensusDisambiguator(List<PhraseAnalyzer> phraseAnalyzers){
        this.phraseAnalyzers=phraseAnalyzers;
    }

    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException{
        LinkedHashMap<LocalId, Integer> results = new LinkedHashMap<LocalId, Integer>();
        for (PhraseAnalyzer phraseAnalyzer : phraseAnalyzers){
            LinkedHashMap<LocalPage, Float> localMap = phraseAnalyzer.resolveLocal(phrase.getLanguage(),phrase.getString(),1);
            if (localMap==null||localMap.isEmpty()){
                continue;
            }
            LocalPage localPage = localMap.keySet().iterator().next();
            LocalId localId = localPage.toLocalId();
            if (results.containsKey(localId)){
                results.put(localId,results.get(localId)+1);
            }
            else {
                results.put(localId,1);
            }
        }
        if (results.isEmpty()){
            return null;
        }
        else {
            LocalId best=null;
            int score = 0;
            for (LocalId localId : results.keySet()){
                if (results.get(localId)>score){
                    score = results.get(localId);
                    best = localId;
                }
            }
            return best;
        }
    }

    public List<LocalId> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException{
        List<LocalId> ids = new ArrayList<LocalId>();
        for (LocalString phrase : phrases){
            ids.add(disambiguate(phrase, context));
        }
        return ids;
    }


    public static class Provider extends org.wikapidia.conf.Provider<Disambiguator>{
        public Provider (Configurator configurator, Configuration config) throws ConfigurationException {
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
        public Disambiguator get(String name, Config config) throws  ConfigurationException{
            if (!config.getString("type").equals("topResultConsensus")){
                return null;
            }
            List<PhraseAnalyzer> phraseAnalyzers = new ArrayList<PhraseAnalyzer>();
            for (String analyzer : config.getStringList("phraseAnalyzers")){
                phraseAnalyzers.add(getConfigurator().get(PhraseAnalyzer.class,analyzer));
            }
            return new TopResultConsensusDisambiguator(phraseAnalyzers);
        }
    }
}
