package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import gnu.trove.set.TIntSet;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.ClosestPointIndex;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estimates the number of kilometers between geometries.
 *
 * TODO: handle non-points for neighbors.
 *
 * @author Shilad Sen
 */
public class GeodeticDistanceMetric implements SpatialDistanceMetric {
    private static final double EARTH_RADIUS = 6371.0 * 1000;   // radius of the earth in meters

    private static final Logger LOG = LoggerFactory.getLogger(SpatialDistanceMetric.class);
    private final ClosestPointIndex index;
    private final SpatialDataDao spatialDao;
    private TIntSet concepts;
    private boolean useBorders = false;

    /**
     * Creates a new geodetic spatial distance metric.
     *
     * If useBorder is true, it computes the distance between
     * (multi)polygons by finding the closest two points on the boundary.
     *
     * Otherwise it computes distances between centroids.
     *
     * TODO: consider other methods (e.g. capitials,population-weighted centroids, etc).
     *
     * @param spatialDao
     * @param useBorders
     */
    public GeodeticDistanceMetric(SpatialDataDao spatialDao, ClosestPointIndex index, boolean useBorders) {
        this.spatialDao = spatialDao;
        this.index = index;
        this.useBorders = useBorders;
    }

    public GeodeticDistanceMetric(SpatialDataDao spatialDao, SphericalDistanceMetric spherical) {
        this(spatialDao, spherical.getIndex(), false);
    }

    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    /**
     * TODO: handle non-point geometries.
     * @param enable
     * @throws DaoException
     */
    @Override
    public void enableCache(boolean enable) throws DaoException {
        // Do nothing, for now
    }

    @Override
    public String getName() {
        return "geodetic distance metric";
    }

    @Override
    public double distance(Geometry g1, Geometry g2) {
        return distance(new GeodeticCalculator(), g1, g2);
    }

    @Override
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries) {
        GeodeticCalculator calc = new GeodeticCalculator();
        float [][] matrix = new float[rowGeometries.size()][colGeometries.size()];
        for (int i = 0; i < rowGeometries.size(); i++) {
            for (int j = 0; j < colGeometries.size(); j++) {
                if (rowGeometries.get(i) == colGeometries.get(j)) {
                    matrix[i][j] = 0f;
                } else {
                    matrix[i][j] = (float) distance(calc, rowGeometries.get(i), colGeometries.get(j));
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
     * Returns the closest points to a particular geometry.
     * Note that this ONLY currently uses centroids (FIXME).
     *
     * @param g
     * @param maxNeighbors
     * @param maxDistance
     * @return
     */
    @Override
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance) {
        Point c = WikiBrainSpatialUtils.getCenter(g);
        GeodeticCalculator calc = new GeodeticCalculator();
        calc.setStartingGeographicPoint(c.getX(), c.getY());
        List<Neighbor> results = new ArrayList<Neighbor>();
        for (ClosestPointIndex.Result r: index.query(g, maxNeighbors)) {
            if (r.distance < maxDistance) {
                double kms;
                try {
                    calc.setDestinationGeographicPoint(r.point.getX(), r.point.getY());
                    kms = calc.getOrthodromicDistance();
                } catch (ArithmeticException e) {
                    kms = r.distance;
                } catch (IllegalArgumentException e) {
                    kms = r.distance;
                }
                if (kms <= maxDistance) {
                    results.add(new Neighbor(r.id, kms));
                }
            }
        }
        Collections.sort(results);
        if (results.size() > maxNeighbors) {
            results = results.subList(0, maxNeighbors);
        }
        return results;
    }

    public Geometry cleanupGeometry(Geometry g) {
        if (!(g instanceof MultiPolygon)) {
            return g;
        }
        Geometry largest = null;
        double largestArea = -1;
        MultiPolygon mp = (MultiPolygon)g;
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            Geometry g2 = mp.getGeometryN(i);
            double area = g2.getArea();
            if (area > largestArea) {
                largestArea = area;
                largest = g2;
            }
        }
        return largest;
    }

    private double distance(GeodeticCalculator calc, Geometry g1, Geometry g2) {
        try {
            if (useBorders) {
                g1 = cleanupGeometry(g1);
                g2 = cleanupGeometry(g2);
                Coordinate[] pair = DistanceOp.nearestPoints(g1, g2);
                calc.setStartingGeographicPoint(pair[0].x, pair[0].y);
                calc.setDestinationGeographicPoint(pair[1].x, pair[1].y);
            } else {
                Point p1 = WikiBrainSpatialUtils.getCenter(g1);
                Point p2 = WikiBrainSpatialUtils.getCenter(g2);
                calc.setStartingGeographicPoint(p1.getX(), p1.getY());
                calc.setDestinationGeographicPoint(p2.getX(), p2.getY());
            }
            return calc.getOrthodromicDistance();
        } catch (ArithmeticException e) {
            Point p1 = WikiBrainSpatialUtils.getCenter(g1);
            Point p2 = WikiBrainSpatialUtils.getCenter(g2);
            return WikiBrainSpatialUtils.haversine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        } catch (IllegalArgumentException e) {
            Point p1 = WikiBrainSpatialUtils.getCenter(g1);
            Point p2 = WikiBrainSpatialUtils.getCenter(g2);
            return WikiBrainSpatialUtils.haversine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
    }
}
