package org.wikibrain.sr.disambig;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.phrases.PhraseAnalyzer;

import java.util.*;

/**
 * Resolves disambiguations by naively choosing the most common
 * meaning of a phrase without regards to its context.
 */
public class TopResultDisambiguator extends Disambiguator{
    private final PhraseAnalyzer phraseAnalyzer;

    public TopResultDisambiguator(PhraseAnalyzer phraseAnalyzer){
        this.phraseAnalyzer=phraseAnalyzer;
    }

    @Override
    public List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LinkedHashMap<LocalId, Float>> results = new ArrayList<LinkedHashMap<LocalId, Float>>();
        for (LocalString phrase : phrases) {
            LinkedHashMap<LocalId, Float> localMap = phraseAnalyzer.resolve(phrase.getLanguage(), phrase.getString(), 10);
            if (localMap==null){
                results.add(null);
            } else {
                LinkedHashMap<LocalId, Float> phraseResult = new LinkedHashMap<LocalId, Float>();
                for (LocalId id : localMap.keySet()) {
                    phraseResult.put(id, localMap.get(id));
                }
                results.add(phraseResult);
            }
        }
        return results;
    }

    public static class Provider extends org.wikibrain.conf.Provider<Disambiguator>{
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
