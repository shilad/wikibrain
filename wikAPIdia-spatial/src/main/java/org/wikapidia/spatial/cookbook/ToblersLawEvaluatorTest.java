package org.wikapidia.spatial.cookbook;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.spatial.core.dao.SpatialDataDao;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
        Map<Integer, Geometry> allGeometries = sdDao.getAllGeometries("wikidata", "earth");
        Map<Integer, Geometry> geometryMap = new HashMap<Integer, Geometry>();
        int counter = 0;
        for(Integer id: allGeometries.keySet()){
            geometryMap.put(id, allGeometries.get(id));
            counter ++;
            if(counter >= 100)
                break;
        }
        evaluator.retrieveLocations(geometryMap);

        evaluator.evaluateAll(new File("testTFL.csv"));
        //evaluator.evaluateSample(new File("testTFL.csv"), 100);
    }


}
