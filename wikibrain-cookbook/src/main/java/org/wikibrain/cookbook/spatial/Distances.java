package org.wikibrain.cookbook.spatial;

import com.vividsolutions.jts.geom.Point;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.GraphDistanceMetric;
import org.wikibrain.spatial.distance.SpatialDistanceMetric;
import org.wikibrain.spatial.distance.SphericalDistanceMetric;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;

/**
 * @author Shilad Sen
 */
public class Distances {
    public static void main(String args[]) throws ConfigurationException, DaoException, WikiBrainException {
        Env env = EnvBuilder.envFromArgs(args);
        SpatialDataDao spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        UniversalPageDao conceptDao = env.getConfigurator().get(UniversalPageDao.class);

        SphericalDistanceMetric spherical = new SphericalDistanceMetric(spatialDao);
        spherical.enableCache(true);
        GraphDistanceMetric graph = new GraphDistanceMetric(spatialDao, spherical);
        graph.enableCache(true);

        Point p = WikiBrainSpatialUtils.getPoint(44.916140, -93.266512);
        System.err.println("matches for " + p);
        for (SpatialDistanceMetric.Neighbor n : graph.getNeighbors(p, 100)) {
            UniversalPage page = conceptDao.getById(n.conceptId);
            Title title = page.getBestEnglishTitle(pageDao, true);
            System.err.println("neighbor " + title + " has distance " + n.distance);
        }
    }
}
