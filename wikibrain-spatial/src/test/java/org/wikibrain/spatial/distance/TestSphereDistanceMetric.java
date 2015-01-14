package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
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
public class TestSphereDistanceMetric {
    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(),8307);

    @Ignore
    @Test
    public void testKnnManyTimes() throws DaoException {
        for (int i= 0; i < 100; i++) {
            testKnn();
        }
    }

    @Test
    public void testKnn() throws DaoException {
        int numNeighbors = 100;
        Point query = makePoint();
        Scoreboard<Point> actual = new Scoreboard<Point>(numNeighbors, Scoreboard.Order.INCREASING);

        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        for (int i = 0; i < 100000; i++) {
            Point p = makePoint();
            points.put(i * 3, p);
            actual.add(p, WikiBrainSpatialUtils.haversine(query, p));
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);

        SphericalDistanceMetric metric = new SphericalDistanceMetric(dao);
        metric.enableCache(true);

        System.out.println("Closest points to " + query + " are: ");
        long before = System.currentTimeMillis();
        List<SpatialDistanceMetric.Neighbor> neighbors = metric.getNeighbors(query, numNeighbors);
        long after = System.currentTimeMillis();
        System.err.println("elapsed millis is " + (after - before));

        for (int i = 0; i < neighbors.size(); i++) {
            SpatialDistanceMetric.Neighbor n = neighbors.get(i);
            Point p = actual.getElement(i);
            assertSame(p, points.get(n.conceptId));
            System.out.println("\t" + points.get(n.conceptId) + " with distance " + n.distance);
        }
    }

    @Test
    public void testMatrix() throws DaoException {

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(new HashMap<Integer, Geometry>());

        SphericalDistanceMetric metric = new SphericalDistanceMetric(dao);
        metric.enableCache(true);

        List<Geometry> cols = new ArrayList<Geometry>();
        List<Geometry> rows = new ArrayList<Geometry>();
        for (int i = 0; i < 10; i++) {
            cols.add(makePoint());
        }
        for (int i = 0; i < 5; i++) {
            rows.add(makePoint());
        }
        float [][] distance = metric.distance(rows, cols);
        for (int i = 0; i < cols.size(); i++) {
            for (int j = 0; j < rows.size(); j++) {
                System.err.println("i " +i + ", j " + j);
                assertEquals(metric.distance(rows.get(j), cols.get(i)), distance[j][i], 1.5);
            }
        }
    }

    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }

    @Test
    public void testSphereOrdering() {
        // Make sure ordering for euclidean and spherical distances are consistent.
        for (int i = 0; i < 10000; i++) {
            Point p1 = makePoint();
            Point p2 = makePoint();
            Point p3 = makePoint();

            double d12 = WikiBrainSpatialUtils.haversine(p1, p2);
            double d13 = WikiBrainSpatialUtils.haversine(p1, p3);
            double d23 = WikiBrainSpatialUtils.haversine(p2, p3);

            double e12 = euclidean(p1, p2);
            double e13 = euclidean(p1, p3);
            double e23 = euclidean(p2, p3);

            assert((d12 > d13) == (e12 > e13));
            assert((d12 > d23) == (e12 > e23));
            assert((d13 > d23) == (e13 > e23));
        }
    }

    private double euclidean(Point p1, Point p2) {
        double [] c1 = WikiBrainSpatialUtils.get3DPoints(p1);
        double [] c2 = WikiBrainSpatialUtils.get3DPoints(p2);
        return Math.sqrt(
                (c1[0] - c2[0]) * (c1[0] - c2[0]) +
                (c1[1] - c2[1]) * (c1[1] - c2[1]) +
                (c1[2] - c2[2]) * (c1[2] - c2[2])
            );
    }

    @Test
    public void benchHaversine() {
        double sum = 0.0;
        long before = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            Point p1 = makePoint();
            Point p2 = makePoint();
            sum += WikiBrainSpatialUtils.haversine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
        long after = System.currentTimeMillis();
        System.err.println("elapsed is " + (after-before));
    }
}
