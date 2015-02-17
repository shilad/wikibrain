package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.io.File;
import java.util.*;

/**
 * Created by toby on 4/17/14.
 *
 */
public class ToblersLawEvaluatorTest {


    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        ToblersLawEvaluator evaluator = new ToblersLawEvaluator(env, new LanguageSet("simple"));
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        Map<Integer, Geometry> allGeometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        Map<Integer, Geometry> geometryMap = new HashMap<Integer, Geometry>();
        int counter = 0;




        for(Integer id: allGeometries.keySet()){
            geometryMap.put(id, allGeometries.get(id));
            counter ++;
            if(counter >= 100)
                break;
        }
        //evaluator.retrieveLocations(allGeometries);
        evaluator.retrieveAllLocations();
        //evaluator.evaluateAll(new File("testTFL.csv"));
        evaluator.evaluateSample(new File("testTFL-2.csv"), 1000000);
    }


}
