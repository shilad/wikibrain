package org.wikibrain.spatial.distance;

import ags.utils.dataStructures.MaxHeap;
import ags.utils.dataStructures.trees.thirdGenKD.KdTree;
import ags.utils.dataStructures.trees.thirdGenKD.SquareEuclideanDistanceFunction;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.set.TIntSet;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.ClosestPointIndex;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estimates the number of kilometers between geometries.
 *
 * @author Shilad Sen
 */
public class SphericalDistanceMetric implements SpatialDistanceMetric {

    private static final Logger LOG = LoggerFactory.getLogger(SpatialDistanceMetric.class);
    private ClosestPointIndex index;
    private final SpatialDataDao spatialDao;
    private TIntSet concepts;

    public SphericalDistanceMetric(SpatialDataDao spatialDao) {
        this.spatialDao = spatialDao;
    }

    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    public TIntSet getValidConcepts() {
        return concepts;
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
        index = new ClosestPointIndex();
        final Map<Integer, Geometry> points = this.spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);
        ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
            @Override
            public void call(Integer conceptId) throws Exception {
                if (concepts != null && !concepts.contains(conceptId)) {
                    return;
                }
                index.insert(conceptId, points.get(conceptId));
            }
        }, 100000);
        LOG.info("loaded " + index.size() + " points");
    }

    public int getNumConcepts() {
        return index.size();
    }

    @Override
    public String getName() {
        return "spherical distance metric";
    }

    @Override
    public double distance(Geometry g1, Geometry g2) {
        return WikiBrainSpatialUtils.haversine(WikiBrainSpatialUtils.getCenter(g1), WikiBrainSpatialUtils.getCenter(g2));
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
        List<Neighbor> results = new ArrayList<Neighbor>();
        for (ClosestPointIndex.Result r : index.query(g, maxNeighbors)) {
            if (r.distance <= maxDistance) {
                results.add(new Neighbor(r.id, r.distance));
            }
        }
        Collections.sort(results);
        return results;
    }

    public ClosestPointIndex getIndex() {
        return index;
    }
}
