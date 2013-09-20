package org.wikapidia.sr.ensemble;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;

import java.util.ArrayList;
import java.util.List;

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
    public SRResult predictMostSimilar(List<SRResult> scores) {
        double result=0.0;
        for (SRResult score : scores){
            result+=score.getScore();
        }
        result/=scores.size();
        return new SRResult(result);
    }

    @Override
    public void read(String file) {}

    @Override
    public void write(String file) {}


    public static class Provider extends org.wikapidia.conf.Provider<Ensemble>{
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
        public Ensemble get(String name, Config config) throws ConfigurationException{
            if (!config.getString("type").equals("even")){
                return null;
            }
            return new EvenEnsemble();
        }
    }
}
