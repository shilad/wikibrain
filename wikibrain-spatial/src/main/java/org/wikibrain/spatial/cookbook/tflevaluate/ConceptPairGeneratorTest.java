package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.dao.live.LocalLinkLiveDao;
//import org.wikibrain.core.jooq.tables.UniversalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;
import  org.wikibrain.core.model.UniversalPage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by maixa001 on 6/13/14.
 */
public class ConceptPairGeneratorTest {
    public static void main(String[] args) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
        SpatialDataDao sdDao = c.get(SpatialDataDao.class);
        UniversalPageDao upDao = c.get(UniversalPageDao.class);
        LocalPageDao lDao = c.get(LocalPageDao.class);
        WikidataDao wdao = c.get(WikidataDao.class);
        ConceptPairGenerator pairGenerator = null;
        pairGenerator = new ConceptPairGenerator(env);

//        pairGenerator.loadGeometries(new File("significantGeo160.txt"), new File("significantGeo1000.txt"));

        //create a new point
//        Coordinate[] coords = new Coordinate[1];
//        coords[0] = new Coordinate(-93.26439,44.988113);
//        CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
//        Point here = new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326)); //SRID


//        List<Integer> states= spatialConceptByRegionCreator.getStatesByCountry(30);
//        List<Integer> states= spatialConceptByRegionCreator.getAllStates(here,500);

//        List<Integer> citiesNearHere= spatialConceptByRegionCreator.getAllCitiesWithinDistance(here,500);
//        for(Integer i:citiesNearHere){
//            try{
//                UniversalPage upage=upDao.getById(i,1);
//                System.out.println(upage.getBestEnglishTitle(lDao,true));
//            }catch(Exception e){
//
//            }
//        }

//        List<int[]> pairs = pairGenerator.generateSurvey(here, 160, 1000, 1000);
//        for (int[] pair:pairs){
//            try {
//                UniversalPage u1 = upDao.getById(pair[0], 1);
//                UniversalPage u2 = upDao.getById(pair[1], 1);
//                System.out.println(u1.getBestEnglishTitle(lDao, true).getCanonicalTitle() + " " + u2.getBestEnglishTitle(lDao, true).getCanonicalTitle());
//            } catch (Exception e){
//                System.out.println(pair[0]+" "+pair[1]);
//            }
//        }
//        for (int[] pair: pairs){
//            System.out.println(pair[0]+" "+ pair[1]);
//        }

//        pairGenerator.extractSignificantGeometries(1000);
    }
}


