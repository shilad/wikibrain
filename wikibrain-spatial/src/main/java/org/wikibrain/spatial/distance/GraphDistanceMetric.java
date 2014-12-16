package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.util.ClosestPointIndex;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


/**
 * Estimates the number of kilometers between geometries.
 *
 * @author Shilad Sen
 */
public class GraphDistanceMetric implements SpatialDistanceMetric {
    private static final Logger LOG = Logger.getLogger(GraphDistanceMetric.class.getName());

    private final SpatialDataDao spatialDao;
    private final TIntObjectMap<TIntSet> adjacencyList = new TIntObjectHashMap<TIntSet>();
    private final ClosestPointIndex index;
    private int numNeighbors = 20;
    private int maxDistance = 10;
    private TIntSet concepts;

    public GraphDistanceMetric(SpatialDataDao dao, ClosestPointIndex index) {
        this.spatialDao = dao;
        this.index = index;
    }

    public GraphDistanceMetric(SpatialDataDao dao, SphericalDistanceMetric spherical) {
        this.spatialDao = dao;
        this.index = spherical.getIndex();
    }

    public void setNumNeighbors(int numNeighbors) {
        this.numNeighbors = numNeighbors;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    @Override
    public void enableCache(boolean enable) throws DaoException {
        final AtomicInteger numEdges = new AtomicInteger();
        final Map<Integer, Geometry> points = this.spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);
        ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
                    @Override
                    public void call(Integer conceptId) throws Exception {
                        if (concepts != null && !concepts.contains(conceptId)) {
                            return;
                        }
                        TIntSet neighbors = new TIntHashSet();
                        for (ClosestPointIndex.Result r : index.query(points.get(conceptId), numNeighbors)) {
                            neighbors.add(r.id);
                        }
                        numEdges.addAndGet(neighbors.size());
                        synchronized (adjacencyList) {
                            adjacencyList.put(conceptId, neighbors);
                        }
                    }
                }, 50000);
        LOG.info("Found " + adjacencyList.size() + " edges and " + numEdges.get() + " edges.");
    }

    @Override
    public String getName() {
        return "geodetic distance metric";
    }


    @Override
    public double distance(Geometry g1, Geometry g2) {
        if (adjacencyList.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        // Hack: Replace g2 with CLOSEST concept
        List<ClosestPointIndex.Result> closest = index.query(g2, 1);
        int maxSteps = maxDistance;
        if (maxSteps == 0 || closest.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        if (g1 == g2 || g1.equals(g2)) {
            return 0;
        }

        int targetId = closest.get(0).id;

        TIntSet seen = new TIntHashSet();
        TIntLinkedList queue = new TIntLinkedList();
        for (ClosestPointIndex.Result n : index.query(g1, numNeighbors)) {
            if (n.id== targetId) {
                return 1;
            }
            queue.add(n.id);
            seen.add(n.id);
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
//            System.err.println("at level " + level + " saw " + seen.size());
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
        for (ClosestPointIndex.Result r : index.query(g, numNeighbors)) {
            queue.add(r.id);
            result.add(new Neighbor(r.id, 1));
            seen.add(r.id);
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
