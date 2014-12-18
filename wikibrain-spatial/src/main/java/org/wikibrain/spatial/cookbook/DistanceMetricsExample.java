package org.wikibrain.spatial.cookbook;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
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

/**
 * Created by toby on 5/17/14.
 */
public class DistanceMetricsExample {

    public static void main(String[] args) throws ConfigurationException, DaoException{

        DistanceMetrics distanceMetrics = new DistanceMetrics();

        Env env = new EnvBuilder().build();

        Configurator c = env.getConfigurator();
        SpatialNeighborDao snDao = c.get(SpatialNeighborDao.class);
        WikidataDao wdDao = c.get(WikidataDao.class);
        LocalPageDao lpDao = c.get(LocalPageDao.class);
        SpatialDataDao sdDao = c.get(SpatialDataDao.class);


        LanguageSet loadedLangs = lpDao.getLoadedLanguages();

        // set up the parameters for the call to getContainedItemIds
        String originName = "University of Minnesota";
        String endName = "Wisconsin";
        String layerName = "wikidata";





        LocalPage originLP = lpDao.getByTitle(new Title(originName, Language.getByLangCode("simple")), NameSpace.ARTICLE);
        LocalPage endLP = lpDao.getByTitle(new Title(endName, Language.getByLangCode("simple")), NameSpace.ARTICLE);

        Integer originId = wdDao.getItemId(originLP);
        Integer endId = wdDao.getItemId(endLP);

        Geometry originPoint = sdDao.getGeometry(originId, layerName, "earth");
        Geometry endPoint = sdDao.getGeometry(endId, layerName, "earth");

        System.out.println("Straight-line distance " + distanceMetrics.getDistance(originPoint, endPoint));
        System.out.println("KNN Distance " + distanceMetrics.getTopologicalDistance(originPoint, originId, endPoint, endId, 1, layerName, "earth"));



    }

}
