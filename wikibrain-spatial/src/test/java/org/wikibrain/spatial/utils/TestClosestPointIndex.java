package org.wikibrain.spatial.utils;


import com.vividsolutions.jts.geom.*;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.distance.SpatialDistanceMetric;
import org.wikibrain.spatial.distance.SphericalDistanceMetric;
import org.wikibrain.spatial.util.ClosestPointIndex;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.utils.Scoreboard;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Shilad Sen
 */
public class TestClosestPointIndex {
    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 8307);

    @Ignore
    @Test
    public void testKnnManyTimes() throws DaoException {
        for (int i = 0; i < 100; i++) {
            testKnn();
        }
    }

    @Test
    public void testKnn() throws DaoException {
        int numNeighbors = 100;
        Point query = makePoint();
        Scoreboard<Point> actual = new Scoreboard<Point>(numNeighbors, Scoreboard.Order.INCREASING);
        ClosestPointIndex index = new ClosestPointIndex();

        for (int i = 0; i < 100000; i++) {
            Point p = makePoint();
            actual.add(p, WikiBrainSpatialUtils.haversine(query, p));
            index.insert(i * 3, p);
        }

        System.out.println("Closest points to " + query + " are: ");
        long before = System.currentTimeMillis();
        List<ClosestPointIndex.Result> neighbors = index.query(query, numNeighbors);
        long after = System.currentTimeMillis();
        System.err.println("elapsed millis is " + (after - before));

        for (int i = 0; i < neighbors.size(); i++) {
            ClosestPointIndex.Result n = neighbors.get(i);
            Point p = actual.getElement(i);
            assertSame(p, n.point);
            System.out.println("\t" + n.point + " with distance " + n.distance);
        }
    }

    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }
}
