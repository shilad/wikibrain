package org.wikibrain.spatial.cookbook;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.GeodeticDistanceMetric;
import org.wikibrain.spatial.distance.GraphDistanceMetric;
import org.wikibrain.spatial.distance.SphericalDistanceMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class GraphDistanceExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Language lang = env.getDefaultLanguage();

        SpatialDataDao spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        UniversalPageDao conceptDao = env.getConfigurator().get(UniversalPageDao.class);

        SphericalDistanceMetric metric1 = new SphericalDistanceMetric(spatialDao);
        metric1.enableCache(true);
        GraphDistanceMetric metric2 = new GraphDistanceMetric(spatialDao, metric1);
        metric2.enableCache(true);

        Map<Integer, Geometry> points = spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);
        List<Integer> ids = new ArrayList<Integer>(points.keySet());

        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            int univId1 = ids.get(random.nextInt(ids.size()));
            int univId2 = ids.get(random.nextInt(ids.size()));
            LocalPage page1 = pageDao.getById(lang, conceptDao.getLocalId(lang, univId1));
            LocalPage page2 = pageDao.getById(lang, conceptDao.getLocalId(lang, univId2));
            double distance = metric2.distance(points.get(univId1), points.get(univId2));
            System.out.println("distance between " + page1 + " and " + page2 + " is " + distance);
        }

    }
}
