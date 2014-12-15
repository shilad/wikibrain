package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.junit.Before;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Shilad Sen
 */
public class TestGraphDistanceMetric {

    private static final int LATTICE_ROWS = 5;
    private static final int LATTICE_COLS = 100;

    private Random random = new Random();
    private GeometryFactory factory = new GeometryFactory(new PrecisionModel(),8307);
    private Point[][] lattice = new Point[LATTICE_ROWS][LATTICE_COLS];

    @Before
    public void makeLattice() {
        for (int i = 0; i < LATTICE_ROWS; i++) {
            for (int j = 0; j < LATTICE_COLS; j++) {
                lattice[i][j] = factory.createPoint(new Coordinate(j, i));
            }
        }
    }

    private GraphDistanceMetric getLatticeMetric() throws DaoException {
        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        for (int i = 0; i < LATTICE_ROWS; i++) {
            for (int j = 0; j < LATTICE_COLS; j++) {
                points.put(points.size(), lattice[i][j]);
            }
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);

        SphericalDistanceMetric metric1 = new SphericalDistanceMetric(dao);
        metric1.enableCache(true);

        GraphDistanceMetric metric2 = new GraphDistanceMetric(dao, metric1);
        metric2.enableCache(true);
        metric2.setMaxDistance(10);
        metric2.setNumNeighbors(25);

        return metric2;
    }

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

    @Test
    public void testLattice() throws DaoException {
        GraphDistanceMetric metric = getLatticeMetric();

        // Test distances from middle row, first column to other rows in first column
        assertEquals(1.0, metric.distance(lattice[2][0], lattice[0][0]), 0.01);
        assertEquals(1.0, metric.distance(lattice[2][0], lattice[1][0]), 0.01);
        assertEquals(0.0, metric.distance(lattice[2][0], lattice[2][0]), 0.01);
        assertEquals(1.0, metric.distance(lattice[2][0], lattice[3][0]), 0.01);
        assertEquals(1.0, metric.distance(lattice[2][0], lattice[4][0]), 0.01);

        /*
         * Distances from target node:
         *
         * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9
         *
         * 1 1 1 1 1 2 2 2 3 3 3 4 4 4 5 5 5 6 6 6
         * 1 1 1 1 1 2 2 2 3 3 3 4 4 5 5 5 5 6 6 7
         * 0 1 1 1 1 2 2 3 3 3 4 4 4 5 5 5 6 6 6 7
         * 1 1 1 1 1 2 2 2 3 3 3 4 4 4 5 5 5 6 6 6
         * 1 1 1 1 1 2 2 2 3 3 3 4 4 4 5 5 5 6 6 6
         */
        for (int i = 0; i < LATTICE_ROWS; i++) {
            for (int j = 0; j < 20; j++) {
                int d = (int) Math.round(metric.distance(lattice[2][0], lattice[i][j]));
                System.err.print(" " + d);
            }
            System.err.println("");
        }

    }



    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }

}
