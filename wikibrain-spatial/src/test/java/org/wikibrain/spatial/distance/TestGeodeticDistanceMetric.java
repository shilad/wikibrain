package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.geotools.referencing.GeodeticCalculator;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.loader.SpatialDataLoader;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.utils.Scoreboard;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Shilad Sen
 */
public class TestGeodeticDistanceMetric {
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
        GeodeticCalculator calc = new GeodeticCalculator();
        calc.setStartingGeographicPoint(query.getX(), query.getY());

        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        int diverged = 0;
        for (int i = 0; i < 100000; i++) {
            Point p = makePoint();
            points.put(i * 3, p);
            calc.setDestinationGeographicPoint(p.getX(), p.getY());
            try {
                actual.add(p, calc.getOrthodromicDistance());
            } catch (ArithmeticException e) {
                diverged++;
            }
        }
        System.err.println("number of diverged points: " + diverged);

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);


        SphericalDistanceMetric spherical = new SphericalDistanceMetric(dao);
        spherical.enableCache(true);
        GeodeticDistanceMetric metric = new GeodeticDistanceMetric(dao, spherical);
        metric.enableCache(true);

        System.out.println("Closest points to " + query + " are: ");
        long before = System.currentTimeMillis();
        List<SpatialDistanceMetric.Neighbor> neighbors = metric.getNeighbors(query, numNeighbors);
        long after = System.currentTimeMillis();
        System.err.println("elapsed millis is " + (after - before));

        for (int i = 0; i < numNeighbors; i++) {
            SpatialDistanceMetric.Neighbor n = neighbors.get(i);
            Point p1 = actual.getElement(i);
            Geometry p2 = (Point)points.get(n.conceptId);

            assertEquals(n.distance, actual.getScore(i), n.distance * 0.005);  // maximum error due to ellipsoid is 0.5%
//            System.out.println("\tgot" + p2 + " with distance " + n.distance + " expected " + p1 + " with distance " + actual.getScore(i));
        }
    }


    @Test
    public void testMatrix() throws DaoException {

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(new HashMap<Integer, Geometry>());

        SphericalDistanceMetric spherical = new SphericalDistanceMetric(dao);
        spherical.enableCache(true);
        GeodeticDistanceMetric metric = new GeodeticDistanceMetric(dao, spherical);

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
//                System.err.println("i " +i + ", j " + j);
                assertEquals(metric.distance(rows.get(j), cols.get(i)), distance[j][i], 1.5);
            }
        }
    }

    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }

}
