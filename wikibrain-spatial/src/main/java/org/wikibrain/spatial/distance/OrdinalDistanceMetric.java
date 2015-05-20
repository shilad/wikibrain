package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Given concepts c1 and c2, the ordinal distance from c1 to c2 is c2's rank in spherical distance.
 * The distance of c1 to itself will be 0; the distance of c1 to it's closest neighbor(s) will be 1, etc.
 *
 * Ordinal distance is calculated exactly up to some threshold of nearest neighbors.
 * The default is to exactly calculate ranks to the closest 5% of geometric entities.
 * Beyond this distance, it is estimated based on the distance from the furthest known
 * neighbor (e.g. the 5% boundary) to the furthest possible neighbor (e.g. earth's circumference / 2).
 *
 * @author Shilad Sen
 */
public class OrdinalDistanceMetric implements SpatialDistanceMetric {

    private static final int MAX_EXACT_DISTANCE = 10000;
    private static final int SAMPLE_SIZE = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(GraphDistanceMetric.class);
    private final SpatialDataDao spatialDao;
    private final TIntObjectMap<TIntSet> adjacencyList = new TIntObjectHashMap<TIntSet>();
    private final SphericalDistanceMetric spherical;
    private TIntSet concepts;

    private double fractionRankedExactly = 0.10;
    private double maxDistance = WikiBrainSpatialUtils.EARTH_CIRCUMFERENCE / 2;


    public OrdinalDistanceMetric(SpatialDataDao dao, SphericalDistanceMetric spherical) throws DaoException {
        this.spatialDao = dao;
        this.spherical = spherical;
    }

    @Override
    public List<SpatialDistanceMetric.Neighbor> getNeighbors(Geometry g, int maxNeighbors) {
        return getNeighbors(g, maxNeighbors, Double.MAX_VALUE);
    }

    @Override
    public List<SpatialDistanceMetric.Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance) {
        int k = maxNeighbors;
        if (concepts != null) {
            k = Math.max(k, maxNeighbors);
        }
        List<SpatialDistanceMetric.Neighbor> sphericalNeighbors = spherical.getNeighbors(g, k, Double.MAX_VALUE);
        List<SpatialDistanceMetric.Neighbor> result = new ArrayList<Neighbor>();
        for (int i = 0; i < sphericalNeighbors.size() && result.size() < maxNeighbors; i++) {
            Neighbor n = sphericalNeighbors.get(i);
            if (concepts == null || concepts.contains(n.conceptId)) {
                result.add(new Neighbor(n.conceptId, i));
            }
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
        double []n = getNeighborDistances(g1);
        double d = spherical.distance(g1, g2);
        return estimateOrdinalDistance(n, d);
    }

    private double estimateOrdinalDistance(double [] neighborDistances, double sphericalDist) {
        int numConcepts = spherical.getNumConcepts();
        if (neighborDistances.length > numConcepts) {
            throw new IllegalStateException();
        }
        double furthest = neighborDistances[neighborDistances.length-1];
        if (sphericalDist >= furthest) {
            double ds = sphericalDist - furthest;
            double maxds = maxDistance - furthest;
            int remainingConcepts = numConcepts - neighborDistances.length;
            return neighborDistances.length + ds / maxds * remainingConcepts;
        } else {
            int i = Arrays.binarySearch(neighborDistances, sphericalDist);
            if (i < 0) {
                i = -i - 1;
            }
            return i;
        }
    }

    private double[] getNeighborDistances(Geometry g) {
        List<Neighbor> neighbors = spherical.getNeighbors(g, getMaxNeighbors());
        double distances[] = new double[neighbors.size()];
        for (int i = 0; i < neighbors.size(); i++) {
            distances[i] = neighbors.get(i).distance;
        }
        return distances;
    }

    private int getMaxNeighbors() {
        return (int) (fractionRankedExactly * spherical.getNumConcepts());
    }

    @Override
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries) {
        int nrows = rowGeometries.size();
        int ncols = colGeometries.size();
        float result[][] = spherical.distance(rowGeometries, colGeometries);
        double rowDists[][] = new double[nrows][];
//        double colDists[][] = new double[ncols][];
        for (int i = 0; i < nrows; i++) {
            rowDists[i] = getNeighborDistances(rowGeometries.get(i));
        }
//        for (int i = 0; i < ncols; i++) {
//            colDists[i] = getNeighborDistances(colGeometries.get(i));
//        }
        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                result[i][j] = (float) estimateOrdinalDistance(rowDists[i], result[i][j]);
            }
        }
        return result;
    }

    @Override
    public float[][] distance(List<Geometry> geometries) {
        return distance(geometries, geometries);
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public double getFractionRankedExactly() {
        return fractionRankedExactly;
    }


}
