package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.sf.jsi.Rectangle;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Estimates the number of kilometers between geometries.
 *
 * @author Shilad Sen
 */
public class GraphDistanceMetric implements SpatialDistanceMetric {
    private final SpatialDataDao spatialDao;
    private final TIntObjectMap<TIntSet> adjacencyList = new TIntObjectHashMap<TIntSet>();
    private final GeodeticDistanceMetric geodetic;
    private int numNeighbors = 100;
    private int maxDistance = 50;

    public GraphDistanceMetric(SpatialDataDao dao, GeodeticDistanceMetric geodetic) throws DaoException {
        this.spatialDao = dao;
        this.geodetic = geodetic;
    }

    public void setNumNeighbors(int numNeighbors) {
        this.numNeighbors = numNeighbors;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public void enableCache(boolean enable) throws DaoException {
        final Map<Integer, Geometry> points = this.spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);
        ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
                    @Override
                    public void call(Integer conceptId) throws Exception {
                        TIntSet neighbors = new TIntHashSet();
                        for (Neighbor n : geodetic.getNeighbors(points.get(conceptId), numNeighbors)) {
                            neighbors.add(n.conceptId);
                        }
                        synchronized (adjacencyList) {
                            adjacencyList.put(conceptId, neighbors);
                        }
                    }
                }, 50000);
    }

    @Override
    public String getName() {
        return "geodetic distance metric";
    }


    @Override
    public double distance(Geometry g1, Geometry g2) {
        // Hack: Replace g2 with CLOSEST concept
        List<Neighbor> closest = geodetic.getNeighbors(g2, 1);
        int maxSteps = maxDistance;
        if (maxSteps == 0 || closest.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        int targetId = closest.get(0).conceptId;

        TIntSet seen = new TIntHashSet();
        TIntLinkedList queue = new TIntLinkedList();
        for (Neighbor n : geodetic.getNeighbors(g1, numNeighbors)) {
            if (n.conceptId == targetId) {
                return 1;
            }
            queue.add(n.conceptId);
            seen.add(n.conceptId);
        }

        for (int level = 2; level <= maxSteps; level++) {
            // Do all nodes at this level
            int nodes = queue.size();
            for (int i = 0; i < nodes; i++) {
                int id = queue.removeAt(0);
                if (!adjacencyList.containsKey(id)) {
                    continue;
                }
                for (int id2 : adjacencyList.get(id).toArray()) {
                    if (id2 == targetId) {
                        return level;
                    }
                    if (!seen.contains(id2)) {
                        queue.add(id2);
                        seen.add(id2);
                    }
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries) {
        return new float[0][];
    }

    @Override
    public float[][] distance(List<Geometry> geometries) {
        return new float[0][];
    }

    @Override
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors) {
        return getNeighbors(g, maxNeighbors, Double.MAX_VALUE);
    }

    @Override
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance) {
        List<Neighbor> result = new ArrayList<Neighbor>();
        int maxSteps = (int) Math.round(maxDistance);
        if (maxSteps == 0) {
            return result;
        }

        TIntSet seen = new TIntHashSet();
        TIntLinkedList queue = new TIntLinkedList();
        for (Neighbor n : geodetic.getNeighbors(g, numNeighbors)) {
            queue.add(n.conceptId);
            result.add(new Neighbor(n.conceptId, 1));
            seen.add(n.conceptId);
        }

        for (int level = 2; level <= maxSteps; level++) {
            // Do all nodes at this level
            int nodes = queue.size();
            for (int i = 0; i < nodes; i++) {
                int id = queue.removeAt(0);
                if (!adjacencyList.containsKey(id)) {
                    continue;
                }
                for (int id2 : adjacencyList.get(id).toArray()) {
                    if (!seen.contains(id2)) {
                        queue.add(id2);
                        result.add(new Neighbor(id2, level));
                        seen.add(id2);
                    }
                    if (result.size() >= maxNeighbors) {
                        break;
                    }
                }
                if (result.size() >= maxNeighbors) {
                    break;
                }
            }
            if (result.size() >= maxNeighbors) {
                break;
            }
        }
        return result;
    }
}
