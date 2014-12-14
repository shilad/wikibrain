package org.wikibrain.spatial.distance;

import ags.utils.dataStructures.MaxHeap;
import ags.utils.dataStructures.trees.thirdGenKD.KdTree;
import ags.utils.dataStructures.trees.thirdGenKD.SquareEuclideanDistanceFunction;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.set.TIntSet;
import org.apache.commons.math3.util.FastMath;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Estimates the number of kilometers between geometries.
 *
 * @author Shilad Sen
 */
public class SphericalDistanceMetric implements SpatialDistanceMetric {

    private static final Logger LOG = Logger.getLogger(SpatialDistanceMetric.class.getName());
    private KdTree<IndexPoint> index;
    private final SpatialDataDao spatialDao;
    private TIntSet concepts;

    public SphericalDistanceMetric(SpatialDataDao spatialDao) {
        this.spatialDao = spatialDao;
    }

    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    /**
     * TODO: handle non-point geometries.
     * @param enable
     * @throws org.wikibrain.core.dao.DaoException
     */
    @Override
    public void enableCache(boolean enable) throws DaoException {
        if (!enable) {
            index = null;
            return;
        }
        index = new KdTree<IndexPoint>(3);
        final Map<Integer, Geometry> points = this.spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);
        ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
            @Override
            public void call(Integer conceptId) throws Exception {
                if (concepts != null && !concepts.contains(conceptId)) {
                    return;
                }

                // create 3-d spherical coordinates
                Point p = WikiBrainSpatialUtils.getCenter(points.get(conceptId));
                double[] coords = get3DPoints(p);
                IndexPoint ip = new IndexPoint();
                ip.conceptId = conceptId;
                ip.point = p;
                synchronized (index) {
                    index.addPoint(coords, ip);
                }
            }
        }, 100000);
        LOG.info("loaded " + index.size() + " points");
    }

    @Override
    public String getName() {
        return "geodetic distance metric";
    }

    @Override
    public double distance(Geometry g1, Geometry g2) {
        return haversine(WikiBrainSpatialUtils.getCenter(g1), WikiBrainSpatialUtils.getCenter(g2));
    }

    @Override
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries) {
        Point [] rowPoints = new Point[rowGeometries.size()];
        Point [] colPoints = new Point[colGeometries.size()];
        for (int i = 0; i < rowGeometries.size(); i++) {
            rowPoints[i] = WikiBrainSpatialUtils.getCenter(rowGeometries.get(i));
        }
        for (int i = 0; i < colGeometries.size(); i++) {
            colPoints[i] = WikiBrainSpatialUtils.getCenter(colGeometries.get(i));
        }
        float [][] matrix = new float[rowGeometries.size()][colGeometries.size()];
        for (int i = 0; i < rowGeometries.size(); i++) {
            for (int j = 0; j < colGeometries.size(); j++) {
                if (rowGeometries.get(i) == colGeometries.get(j) || rowPoints[i].equals(colPoints[j])) {
                    matrix[i][j] = 0f;
                } else {
                    matrix[i][j] = (float) distance(rowPoints[i], colPoints[j]);
                }
            }
        }
        return matrix;
    }

    @Override
    public float[][] distance(List<Geometry> geometries) {
        return distance(geometries, geometries);
    }

    @Override
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors) {
        return getNeighbors(g, maxNeighbors, Double.MAX_VALUE);
    }

    /**
     * A fast approximation of the distance between neighbors based on the 3D straight line distance.
     * @param g
     * @param maxNeighbors
     * @return
     */
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance) {
        final Point c = WikiBrainSpatialUtils.getCenter(g);
        List<Neighbor> results = new ArrayList<Neighbor>();
        for (IndexPoint ip : getNeighborsInternal(g, maxNeighbors, maxDistance)) {
            results.add(new Neighbor(
                    ip.conceptId,
                    haversine(c, ip.point)
            ));
        }
        Collections.sort(results);
        return results;
    }

    protected List<IndexPoint> getNeighborsInternal(Geometry g, int maxNeighbors, double maxDistance) {
        if (index == null) {
            // TODO: get from Toby's code
            throw new UnsupportedOperationException();
        }
        final Point c = WikiBrainSpatialUtils.getCenter(g);
        final double[] c1 = get3DPoints(c);
        MaxHeap<IndexPoint> heap = index.findNearestNeighbors(
                c1,
                maxNeighbors,
                new SquareEuclideanDistanceFunction());

        GeodeticCalculator calc = new GeodeticCalculator();
        calc.setStartingGeographicPoint(c.getX(), c.getY());
        List<IndexPoint> results = new ArrayList<IndexPoint>();
        while (heap.size() > 0) {
            results.add(heap.getMax());
            heap.removeMax();
        }
        Collections.reverse(results);
        return results;
    }

    public static double[] get3DPoints(Point p) {
        double lng = FastMath.toRadians(p.getX());
        double lat = FastMath.toRadians(p.getY());
        return new double[] {
                FastMath.cos(lat) * FastMath.sin(-lng),
                FastMath.cos(lat) * FastMath.cos(-lng),
                FastMath.sin(-lat),
        };
    }

    protected static class IndexPoint {
        public int conceptId;
        public Point point;
    }


    /**
     * Radius of earth, in meters.
     */
    public static final double EARTH_RADIUS = 6372800;

    public static double haversine(Point p1, Point p2) {
        return haversine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Approximation of the distance between two geographic points that treats the
     * earth as a sphere. Fast, but can have 0.5% error because the Earth is closer
     * to an ellipsoid.
     *
     * From http://rosettacode.org/wiki/Haversine_formula#Java
     *
     * The use of FastMath below cuts the time by more than 50%.
     *
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return
     */
    public static double haversine(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = FastMath.sin(dLat / 2) * FastMath.sin(dLat / 2) + FastMath.sin(dLon / 2) * FastMath.sin(dLon / 2) * FastMath.cos(lat1) * FastMath.cos(lat2);
        double c = 2 * FastMath.asin(FastMath.sqrt(a));
        return EARTH_RADIUS * c;
    }
}
