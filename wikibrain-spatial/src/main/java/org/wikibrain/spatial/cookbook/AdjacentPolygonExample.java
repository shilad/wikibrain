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
 * Created by toby on 5/24/14.
 */
public class AdjacentPolygonExample {

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
            String originName = "China";
            String layerName = "country";
            Set<String> subLayers = Sets.newHashSet();
            subLayers.add("wikidata");




            LocalPage lp = lpDao.getByTitle(new Title(originName, Language.getByLangCode("simple")), NameSpace.ARTICLE);
            Integer id = wdDao.getItemId(lp);
            Geometry rootPoint = sdDao.getGeometry(id, layerName, "earth");
            Map<Integer, Geometry> resMap = snDao.getNeighbors(rootPoint, layerName, "earth", new HashSet<Integer>());


            for(Integer i : resMap.keySet()){
                System.out.println(i.toString() + "  " + wdDao.getUniversalPage(i).getBestEnglishTitle(lpDao, true).getCanonicalTitle() + "  " + distanceMetrics.getDistance(resMap.get(i), rootPoint) + " km");
            }


            System.out.println(resMap.keySet().size() + " adjacent polygons found");



        } catch(Exception e){
            e.printStackTrace();
        }



    }
}
