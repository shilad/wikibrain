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

import java.util.*;

/**
 * Resolves disambiguations by naively choosing the most common
 * meaning of a phrase without regards to its context.
 */
public class TopResultDisambiguator implements Disambiguator{
    private final PhraseAnalyzer phraseAnalyzer;

    public TopResultDisambiguator(PhraseAnalyzer phraseAnalyzer){
        this.phraseAnalyzer=phraseAnalyzer;
    }


    @Override
    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException {
        LinkedHashMap<LocalPage, Float> localMap = phraseAnalyzer.resolveLocal(phrase.getLanguage(), phrase.getString(), 1);
        if (localMap==null){
            return null;
        }
        Iterator<LocalPage> pageIterator = localMap.keySet().iterator();
        if (pageIterator.hasNext()){
            LocalPage localPage = pageIterator.next();
//            System.err.println("returinging " + localPage + " for " + phrase);
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

    public static class Provider extends org.wikapidia.conf.Provider<Disambiguator>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super (configurator,config);
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
            if (!config.getString("type").equals("topResult")){
                return null;
            }
            return new TopResultDisambiguator(
                    getConfigurator().get(PhraseAnalyzer.class,
                            config.getString("phraseAnalyzer"))
            );
        }
    }
}
