package org.wikibrain.spatial.distance;


import com.vividsolutions.jts.geom.*;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Shilad Sen
 */
public class TestOrdinalDistanceMetric {
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

        SphericalDistanceMetric spherical = new SphericalDistanceMetric(dao);
        OrdinalDistanceMetric ordinal = new OrdinalDistanceMetric(dao, spherical);

        spherical.enableCache(true);
        ordinal.enableCache(true);

        assertEquals(spherical.getNumConcepts(), points.size());
        int numNeighbors = (int) (spherical.getNumConcepts() * ordinal.getFractionRankedExactly());

        for (int i = 0; i < 100; i++) {
            Point p = makePoint();
            List<SpatialDistanceMetric.Neighbor> sphericalNeighbors = spherical.getNeighbors(p, numNeighbors);
            List<SphericalDistanceMetric.Neighbor> ordinalNeighbors = ordinal.getNeighbors(p, numNeighbors);
            assertEquals(ordinalNeighbors.size(), numNeighbors);
            for (int j = 0; j < numNeighbors; j++) {
                assertEquals(sphericalNeighbors.get(j).conceptId, ordinalNeighbors.get(j).conceptId);
                assertEquals(j, ordinalNeighbors.get(j).distance, 0.01);
            }
        }
    }

    @Test
    public void testPairwise() throws DaoException {

        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        for (int i = 0; i < 10000; i++) {
            points.put(i * 3, makePoint());
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);

        final SphericalDistanceMetric spherical = new SphericalDistanceMetric(dao);
        final OrdinalDistanceMetric ordinal = new OrdinalDistanceMetric(dao, spherical);

        spherical.enableCache(true);
        ordinal.enableCache(true);

        assertEquals(spherical.getNumConcepts(), points.size());
        int numNeighbors = (int) (spherical.getNumConcepts() * ordinal.getFractionRankedExactly());

        for (int i = 0; i < 50; i++) {
            final Point p1 = makePoint();
            assertTrue(ordinal.distance(p1, p1) <= 1.0);
            List<SpatialDistanceMetric.Neighbor> neighbors = spherical.getNeighbors(p1, numNeighbors);
            double furthest = neighbors.get(neighbors.size() - 1).distance;

            List<Point> others = new ArrayList<Point>();
            for (int j = 0; j < 50; j++) {
                others.add(makePoint());
            }

            // Sort others by spherical distance
            Collections.sort(others, new Comparator<Point>() {
                @Override
                public int compare(Point p2, Point p3) {
                    return Double.compare(spherical.distance(p1, p2), spherical.distance(p1, p3));
                }
            });

            double lastDistance = -1;
            for (Point p2 : others) {
                double d = ordinal.distance(p1, p2);
                double sphericald = spherical.distance(p1, p2);
                assertTrue(d >= lastDistance);
                if (sphericald < furthest) {
                    int j = (int)Math.round(d);
                    if (j == 0) {
                        assertTrue(sphericald < neighbors.get(0).distance);
                    } else {
                        assertTrue(sphericald >= neighbors.get(j-1).distance);
                        assertTrue(sphericald <= neighbors.get(j).distance);
                    }
                } else {
                    assertTrue(d >= numNeighbors);
                }
                lastDistance = d;
            }
        }
    }

    @Test
    public void testMatrix() throws DaoException {
        Map<Integer, Geometry> points = new HashMap<Integer, Geometry>();
        for (int i = 0; i < 10000; i++) {
            points.put(i * 3, makePoint());
        }

        SpatialDataDao dao = mock(SpatialDataDao.class);
        when(dao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH))
                .thenReturn(points);

        SphericalDistanceMetric spherical = new SphericalDistanceMetric(dao);
        OrdinalDistanceMetric ordinal = new OrdinalDistanceMetric(dao, spherical);
        spherical.enableCache(true);
        ordinal.enableCache(true);

        List<Geometry> cols = new ArrayList<Geometry>();
        List<Geometry> rows = new ArrayList<Geometry>();
        for (int i = 0; i < 1000; i++) {
            cols.add(makePoint());
        }
        for (int i = 0; i < 1000; i++) {
            rows.add(makePoint());
        }
        float [][] ordinalDistance = ordinal.distance(rows, cols);
        float [][] sphericalDistance = spherical.distance(rows, cols);

        // Check that neighbor comparisons are correctly ordered with respect to
        // spherical distance. This isn't exhaustive, but it should eventually catch
        // errors.
        for (int i = 0; i < rows.size(); i++) {
            for (int j = 1; j < cols.size(); j++) {
                if (sphericalDistance[i][j] < sphericalDistance[i][j-1]) {
                    assertTrue(ordinalDistance[i][j] <= ordinalDistance[i][j-1]);
                } else if (sphericalDistance[i][j] > sphericalDistance[i][j-1]) {
                    assertTrue(ordinalDistance[i][j] >= ordinalDistance[i][j-1]);
                } else {
                    // Otherwise the ordinal distances may differ by one (should basically never happen!)
                    assertTrue(Math.abs(ordinalDistance[i][j] - ordinalDistance[i][j - 1]) <= 1.01);
                }
            }
        }
    }

    private Point makePoint() {
        double lat = 90 - random.nextDouble() * 180;
        double lon = 180 - random.nextDouble() * 360;
        return factory.createPoint(new Coordinate(lon, lat));
    }
}
