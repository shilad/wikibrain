package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import net.sf.jsi.Rectangle;
import net.sf.jsi.rtree.RTree;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.List;
import java.util.Map;

/**
 * Estimates the number of kilometers between geometries.
 *
 * TODO: handle non-points for neighbors.
 *
 * @author Shilad Sen
 */
public class GeodeticDistanceMetric implements SpatialDistanceMetric {
    private RTree index;
    private final SpatialDataDao spatialDao;

    public GeodeticDistanceMetric(SpatialDataDao spatialDao) {
        this.spatialDao = spatialDao;
    }

    /**
     * TODO: handle non-point geometries.
     * @param enable
     * @throws DaoException
     */
    @Override
    public void enableCache(boolean enable) throws DaoException {
        if (!enable) {
            index = null;
            return;
        }
        index = new RTree();
        index.init(null);
        final Map<Integer, Geometry> points = this.spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);
        ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
            @Override
            public void call(Integer conceptId) throws Exception {
                Envelope e = points.get(conceptId)
                        .getEnvelope()
                        .buffer(1 / 10000.0)    // roughly 10m
                        .getEnvelopeInternal();
                Rectangle r = new Rectangle(
                        (float)e.getMinX(), (float)e.getMinY(),
                        (float)e.getMaxX(), (float)e.getMaxY());
                synchronized (index) {
                    index.add(r, conceptId);
                }
            }
        }, 100000);
    }

    @Override
    public String getName() {
        return "geodetic distance metric";
    }

    @Override
    public double distance(Geometry g1, Geometry g2) {
        GeodeticCalculator calc = new GeodeticCalculator();
        return distance(calc, g1, g2);
    }

    @Override
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries) {
        GeodeticCalculator calc = new GeodeticCalculator();
        float [][] matrix = new float[rowGeometries.size()][colGeometries.size()];
        for (int i = 0; i < rowGeometries.size(); i++) {
            matrix[i][i] = 0f;
            for (int j = i+1; j < colGeometries.size(); j++) {
                matrix[i][j] = (float) distance(calc, rowGeometries.get(i), colGeometries.get(j));
                matrix[j][i] = matrix[i][j];
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

    @Override
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance) {
        if (index == null) {
            // TODO: get from Toby's code
            throw new UnsupportedOperationException();
        }
        Point c = g.getCentroid();
        net.sf.jsi.Point p = new net.sf.jsi.Point((float)c.getX(), (float)c.getY());
        return index.nearestN(p, maxNeighbors, (float)maxDistance);
    }

    private double distance(GeodeticCalculator calc, Geometry g1, Geometry g2) {
        Coordinate[] pair = DistanceOp.nearestPoints(g1, g2);
        calc.setStartingGeographicPoint(pair[0].x, pair[0].y);
        calc.setDestinationGeographicPoint(pair[1].x, pair[1].y);
        return calc.getOrthodromicDistance();
    }
}
