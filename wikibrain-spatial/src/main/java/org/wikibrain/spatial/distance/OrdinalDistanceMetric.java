package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class OrdinalDistanceMetric implements SpatialDistanceMetric {

    private static final int MAX_EXACT_DISTANCE = 10000;
    private static final int SAMPLE_SIZE = 1000;

    private static final Logger LOG = Logger.getLogger(GraphDistanceMetric.class.getName());
    private final SpatialDataDao spatialDao;
    private final TIntObjectMap<TIntSet> adjacencyList = new TIntObjectHashMap<TIntSet>();
    private final SphericalDistanceMetric geodetic;
    private TIntSet concepts;

    private final TIntObjectMap<Point> sample = new TIntObjectHashMap<Point>();


    public OrdinalDistanceMetric(SpatialDataDao dao, SphericalDistanceMetric geodetic) throws DaoException {
        this.spatialDao = dao;
        this.geodetic = geodetic;
    }

    @Override
    public List<SpatialDistanceMetric.Neighbor> getNeighbors(Geometry g, int maxNeighbors) {
        return getNeighbors(g, maxNeighbors, Double.MAX_VALUE);
    }

    @Override
    public List<SpatialDistanceMetric.Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance) {
        List<SpatialDistanceMetric.Neighbor> result = geodetic.getNeighbors(g, maxNeighbors, Double.MAX_VALUE);
        for (int i = 0; i < result.size(); i++) {
            Neighbor n = result.get(i);
            result.set(i, new Neighbor(n.conceptId, i));
        }
        return result;
    }


    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    @Override
    public void enableCache(boolean enable) throws DaoException {
    }

    @Override
    public String getName() {
        return "ordinal distance metric";
    }

    @Override
    public double distance(Geometry g1, Geometry g2) {
        return 0;
    }


    @Override
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries) {
        return new float[0][];
    }

    @Override
    public float[][] distance(List<Geometry> geometries) {
        return new float[0][];
    }

}
