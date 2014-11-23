package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.loader.SpatialDataLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Shilad Sen
 */
public class TestGeodeticDistanceMetric {
    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(),8307);

    @Test
    public void testKnn() throws DaoException {
        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        for (int i = 0; i < 100000; i++) {
            points.put(i * 3, makePoint());
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);

        GeodeticDistanceMetric metric = new GeodeticDistanceMetric(dao);
        metric.enableCache(true);

        Point query = makePoint();
        System.out.println("Closest points to " + query + " are: ");
        for (SpatialDistanceMetric.Neighbor n : metric.getNeighbors(query, 10)) {
            System.out.println("\t" + points.get(n.conceptId) + " with distance " + n.distance);
        }
    }

    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }

}
