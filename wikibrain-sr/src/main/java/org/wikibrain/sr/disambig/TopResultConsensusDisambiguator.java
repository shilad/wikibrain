package org.wikibrain.sr.disambig;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.*;

/**
 * @author Matt Lesicko
 */
public class TopResultConsensusDisambiguator extends Disambiguator {
    private List<PhraseAnalyzer> phraseAnalyzers;

    public TopResultConsensusDisambiguator(List<PhraseAnalyzer> phraseAnalyzers){
        this.phraseAnalyzers=phraseAnalyzers;
    }

    public LocalId disambiguateTop(LocalString phrase, Set<LocalString> context) throws DaoException{
        LinkedHashMap<LocalId, Integer> results = new LinkedHashMap<LocalId, Integer>();
        for (PhraseAnalyzer phraseAnalyzer : phraseAnalyzers){
            LinkedHashMap<LocalId, Float> localMap = phraseAnalyzer.resolve(phrase.getLanguage(), phrase.getString(), 1);
            if (localMap==null||localMap.isEmpty()){
                continue;
            }
            LocalId localId = localMap.keySet().iterator().next();
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

    public List<LocalId> disambiguateTop(List<LocalString> phrases, Set<LocalString> context) throws DaoException{
        List<LocalId> ids = new ArrayList<LocalId>();
        for (LocalString phrase : phrases){
            ids.add(disambiguateTop(phrase, context));
        }
        return ids;
    }

    @Override
    public List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        if (phrases.isEmpty()) {
            return new ArrayList<LinkedHashMap<LocalId, Float>>();
        }
        Language lang = phrases.get(0).getLanguage();
        List<LinkedHashMap<LocalId, Float>> results = new ArrayList<LinkedHashMap<LocalId, Float>>();
        for (LocalString phrase : phrases) {
            Map<Integer, Double> pageSums = new HashMap<Integer, Double>();
            for (PhraseAnalyzer pa : phraseAnalyzers) {
                LinkedHashMap<LocalId, Float> probs = pa.resolve(phrase.getLanguage(), phrase.getString(), 20);
                for (Map.Entry<LocalId, Float> entry : probs.entrySet()) {
                    int id = entry.getKey().getId();
                    if (pageSums.containsKey(id)) {
                        pageSums.put(id, pageSums.get(id) + entry.getValue());
                    } else {
                        pageSums.put(id, (double)entry.getValue());
                    }
                }
            }
            LinkedHashMap<LocalId, Float> pageResult = new LinkedHashMap<LocalId, Float>();
            for (Integer key : WpCollectionUtils.sortMapKeys(pageSums, true)) {
                pageResult.put(new LocalId(lang, key), pageSums.get(key).floatValue());
            }
            results.add(pageResult);
        }
        return results;
    }


    public static class Provider extends org.wikibrain.conf.Provider<Disambiguator>{
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
        public Disambiguator get(String name, Config config, Map<String, String> runtimeParams) throws  ConfigurationException{
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
