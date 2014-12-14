package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Shilad Sen
 */
public class TestGraphDistanceMetric {
    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(),8307);

    @Test
    public void testKnn() throws DaoException {
        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        for (int i = 0; i < 10000; i++) {
            points.put(i * 3, makePoint());
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);

        SphericalDistanceMetric metric1 = new SphericalDistanceMetric(dao);
        metric1.enableCache(true);

        GraphDistanceMetric metric2 = new GraphDistanceMetric(dao, metric1);
        metric2.enableCache(true);

        Point query = makePoint();
        List<SpatialDistanceMetric.Neighbor> neighbors = metric2.getNeighbors(query, Integer.MAX_VALUE, 15);

        for (int i = 0; i < 100; i++) {
            Point p1 = makePoint();
            Point p2 = makePoint();
            System.out.println("distance is " + metric2.distance(p1, p2));

        }
        System.out.println("Num neighbors are: " + neighbors.size());
    }

    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }

}
