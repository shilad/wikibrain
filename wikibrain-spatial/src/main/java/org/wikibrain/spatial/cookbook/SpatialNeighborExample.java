package org.wikibrain.spatial.cookbook;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.spatial.cookbook.tflevaluate.DistanceMetrics;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.dao.SpatialNeighborDao;
import org.wikibrain.wikidata.WikidataDao;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by toby on 4/17/14.
 */
public class SpatialNeighborExample {

    public static void main(String[] args){


        try {

            Env env = new EnvBuilder().build();
            Configurator c = env.getConfigurator();
            SpatialNeighborDao snDao = c.get(SpatialNeighborDao.class);
            WikidataDao wdDao = c.get(WikidataDao.class);
            LocalPageDao lpDao = c.get(LocalPageDao.class);
            SpatialDataDao sdDao = c.get(SpatialDataDao.class);
            DistanceMetrics distanceMetrics = new DistanceMetrics();

            LanguageSet loadedLangs = lpDao.getLoadedLanguages();

            // set up the parameters for the call to getContainedItemIds
            String originName = "South Africa";
            String layerName = "wikidata";
            Set<String> subLayers = Sets.newHashSet();
            subLayers.add("wikidata");




            LocalPage lp = lpDao.getByTitle(new Title(originName, Language.getByLangCode("simple")), NameSpace.ARTICLE);
            Integer id = wdDao.getItemId(lp);
            Geometry rootPoint = sdDao.getGeometry(id, layerName, "earth");
            Map<Integer, Geometry> resMap = snDao.getKNNeighbors(id, 100, layerName, "earth" , new HashSet<Integer>());


            for(Integer i : resMap.keySet()){
                System.out.println(i.toString() + "  " + wdDao.getUniversalPage(i).getBestEnglishTitle(lpDao, true).getCanonicalTitle() + "  " + distanceMetrics.getDistance(resMap.get(i), rootPoint) + " km");
            }


            //TIntSet neighborItemIds = snDao.getNeighboringItemIds(id, layerName, "earth", subLayers, 800, 1000);

            //Point p1 = sdDao.getGeometry(id, layerName, "earth").getCentroid();
            /*

            int counter = 0;
            System.out.println("Items contained in the spatial footprint of the article '" + lp.getTitle() + "' are:");

            for (int cId : neighborItemIds.toArray()){
                UniversalPage univPage = wdDao.getUniversalPage(cId);
                Title t = univPage.getBestEnglishTitle(lpDao, true);
                System.out.println();


                Point p2 = sdDao.getGeometry(cId, "wikidata", "earth").getCentroid();

                GeodeticCalculator geoCalc = new GeodeticCalculator();
                geoCalc.setStartingGeographicPoint(p1.getX(), p1.getY());
                geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
                System.out.println(t.getCanonicalTitle() + " " + geoCalc.getOrthodromicDistance() / 1000);



                counter++;
            }

            System.out.printf("Found %d items\n", counter);
            */




        } catch(Exception e){
            e.printStackTrace();
        }



    }

}
