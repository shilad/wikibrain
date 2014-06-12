package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by harpa003 on 6/12/14.
 */
public class GeometryPrinter {

    private static int WIKIDATA_CONCEPTS = 1;
    public static final int INSTANCE_OF=31;

    private static final Logger LOG = Logger.getLogger(ToblersLawEvaluator.class.getName());


    private static SpatialDataDao sdDao;
    private static LocalPageDao lpDao;
    private static UniversalPageDao upDao;
    public static WikidataDao wDao;



    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        LocalPageDao lpDao= conf.get(LocalPageDao.class);
        UniversalPageDao upDao= conf.get(UniversalPageDao.class);
        WikidataDao wDao= conf.get(WikidataDao.class);
        Map<Integer, Geometry> allGeometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");


        for (Integer conceptId : allGeometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            System.out.println(concept.getBestEnglishTitle(lpDao, true) +" is an instance of: ");
            List<WikidataStatement> listOfStatements =  wDao.getItem(conceptId).getStatements();
            for(WikidataStatement stmt: listOfStatements){
                if(stmt.getProperty() == null)
                    continue;
                if(stmt.getProperty().getId()==INSTANCE_OF){
                    int instanceId = (Integer) stmt.getValue().getValue();
                    UniversalPage instance = upDao.getById(instanceId, WIKIDATA_CONCEPTS);
                    if(instance == null)
                        continue;
                    System.out.println("\t"+instance.getBestEnglishTitle(lpDao, true));
                }
            }
            System.out.println("-----------------------\n");
        }
    }
}
