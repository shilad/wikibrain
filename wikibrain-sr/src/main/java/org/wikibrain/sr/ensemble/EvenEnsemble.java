package org.wikibrain.sr.ensemble;

import com.typesafe.config.Config;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Matt Lesicko
 * This is just a testing class. It should be removed once another Ensemble has been created.
 */
public class EvenEnsemble implements Ensemble{
    public EvenEnsemble(){}

    @Override
    public void trainSimilarity(List<EnsembleSim> simList) {}

    @Override
    public void trainMostSimilar(List<EnsembleSim> simList) {}

    @Override
    public SRResult predictSimilarity(List<SRResult> scores) {
        double result=0.0;
        List<Explanation> explanationList = new ArrayList<Explanation>();
        for (SRResult score : scores){
            result+=score.getScore();
            if (score.getExplanations()!=null&&!score.getExplanations().isEmpty()){
                explanationList.addAll(score.getExplanations());
            }
        }
        result/=scores.size();
        return new SRResult(-2,result,explanationList);
    }

    @Override
    public SRResultList predictMostSimilar(List<SRResultList> scores, int maxResults, TIntSet validIds) {
        int numMetrics = scores.size();
        TIntDoubleHashMap scoreMap = new TIntDoubleHashMap();
        for (SRResultList resultList : scores){
            for (SRResult result : resultList){
                double value = result.getScore()/numMetrics;
                scoreMap.adjustOrPutValue(result.getId(),value,value);
            }
        }
        List<SRResult> resultList = new ArrayList<SRResult>();
        for (int id : scoreMap.keys()){
            resultList.add(new SRResult(id,scoreMap.get(id)));
        }
        Collections.sort(resultList);
        Collections.reverse(resultList);
        SRResultList result = new SRResultList(maxResults);
        for (int i=0; i<maxResults&&i<resultList.size();i++){
            result.set(i,resultList.get(i));
        }
        return result;
    }

    @Override
    public void read(String file) {}

    @Override
    public void write(String file) {}


    public static class Provider extends org.wikibrain.conf.Provider<Ensemble>{
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType(){
            return Ensemble.class;
        }

        @Override
        public String getPath(){
            return "sr.ensemble";
        }

        @Override
        public Ensemble get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException{
            if (!config.getString("type").equals("even")){
                return null;
            }
            return new EvenEnsemble();
        }
    }
}
