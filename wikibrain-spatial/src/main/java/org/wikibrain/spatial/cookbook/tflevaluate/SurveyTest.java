package org.wikibrain.spatial.cookbook.tflevaluate;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.spatial.maxima.GenerateNewSpatialPairs;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SurveyQuestionGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by harpa003 on 6/30/14.
 */
public class SurveyTest {

    private static ArrayList<Integer> BlackListSet;
    private static InstanceOfExtractor extractor;

    public static void main(String[] args) throws ConfigurationException, DaoException, IOException{
        SurveyQuestionGenerator questionGenerator= new SurveyQuestionGenerator();
        Env env= EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
        Language l = Language.getByLangCode(c.get(LanguageSet.class).getLangCodes().get(0));
        extractor= new InstanceOfExtractor(c,l);
        extractor.loadScaleIds();
        ArrayList<Integer> knownIds= new ArrayList<Integer>();
        BlackListSet= new ArrayList<Integer>();
        try{
            addToKnownIds(knownIds);
        } catch (Exception e){

        }

        for (int i=0; i<500; i++){
            List<SpatialConceptPair> pairs=questionGenerator.getConceptPairsToAsk(getKnownIds(),i+2);
            System.out.println(i+": pairs size "+pairs.size());
        }
        for(SpatialConceptPair pair: questionGenerator.allPreviousQList){
            System.out.println("kk times: " + pair.getkkTypeNumbOfTimesAsked()+" ku times: "+pair.getkuTypeNumbOfTimesAsked()+" uu times: " + pair.getuuTypeNumbOfTimesAsked());
        }

//        List<SpatialConceptPair> pairs= questionGenerator.getConceptPairsToAsk(knownIds,0);
//        for(SpatialConceptPair pair: pairs){
//            System.out.println(pair.getFirstConcept().getTitle()+"\t\t "+pair.getSecondConcept().getTitle());
//        }
    }

    private static ArrayList<Integer> getKnownIds(){
        ArrayList<Integer> toReturn= new ArrayList<Integer>();
        for (int i = 0; i < 200; i++) {
            toReturn.add(BlackListSet.get((int) (Math.random() * BlackListSet.size())));
        }
        return toReturn;
    }

    private static void addToKnownIds(ArrayList<Integer> knownIds) throws FileNotFoundException{
        Scanner scanner= new Scanner(new File("Blacklist.txt"));
        while(scanner.hasNextInt()){
            int id=scanner.nextInt();
            if(extractor.getScale(id)!=extractor.WEIRD && extractor.getScale(id) != extractor.COUNTY) {
                System.out.println(extractor.getScale(id));
                BlackListSet.add(id);
            }
        }
        scanner.close();
    }
}
