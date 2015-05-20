package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Estimates the number of kilometers between geometries.
 *
 * @author Shilad Sen
 */
public class GraphDistanceMetric implements SpatialDistanceMetric {
    private static final Logger LOG = LoggerFactory.getLogger(GraphDistanceMetric.class);

    private final SpatialDataDao spatialDao;
    private final TIntObjectMap<TIntSet> adjacencyList = new TIntObjectHashMap<TIntSet>();
    private final ClosestPointIndex index;
    private int numNeighbors = 100;
    private int maxDistance = 30;
    private TIntSet concepts;
    private TIntSet validNodes;
    private boolean directed = false;

    public GraphDistanceMetric(SpatialDataDao dao, ClosestPointIndex index) {
        this.spatialDao = dao;
        this.index = index;
    }

    public GraphDistanceMetric(SpatialDataDao dao, SphericalDistanceMetric spherical) {
        this.spatialDao = dao;
        this.index = spherical.getIndex();
        if (spherical.getValidConcepts() != null) {
            LOG.warn("Warning: ClosestPoint index has been constrained to particular concepts. You probably don't want this. Instead, let GraphDistanceMetric create its own index");
        }
    }

    public GraphDistanceMetric(SpatialDataDao dao) {
        this.spatialDao = dao;
        this.index = new ClosestPointIndex();
    }

    public void setNumNeighbors(int numNeighbors) {
        this.numNeighbors = numNeighbors;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    /**
     * Sets the nodes that can be traversed as neighbors.
     * @param nodes
     */
    public void setValidNodes(TIntSet nodes) {
        this.validNodes = nodes;
    }

    /**
     * Sets the nodes that can be RETURNED as neighbors.
     * @param concepts
     */
    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    @Override
    public void enableCache(boolean enable) throws DaoException {
        if (index == null) throw new NullPointerException();
        final AtomicInteger numEdges = new AtomicInteger();
        final Map<Integer, Geometry> points = this.spatialDao.getAllGeometriesInLayer("wikidata", Precision.LatLonPrecision.HIGH);

        // Insert points into the index if necessary.
        if (index.size() == 0) {
            ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                    new Procedure<Integer>() {
                        @Override
                        public void call(Integer conceptId) throws Exception {
                            index.insert(conceptId, points.get(conceptId));
                        }
                    }, 50000);
        }

        ParallelForEach.loop(points.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
                    @Override
                    public void call(Integer conceptId) throws Exception {
                        if (validNodes != null && !validNodes.contains(conceptId)) {
                            return;
                        }
                        final TIntSet neighbors = new TIntHashSet();
                        for (ClosestPointIndex.Result r : index.query(points.get(conceptId), numNeighbors)) {
                            neighbors.add(r.id);
                        }
                        numEdges.addAndGet(neighbors.size());
                        synchronized (adjacencyList) {
                            adjacencyList.put(conceptId, neighbors);
                        }
                    }
                }, 50000);
        // Make links symmetric if necessary
        if (!directed) {
            for (int id1 : adjacencyList.keys()) {
                for (int id2 : adjacencyList.get(id1).toArray()) {
                    if (!adjacencyList.containsKey(id2)) {
                        adjacencyList.put(id2, new TIntHashSet());
                    }
                    adjacencyList.get(id2).add(id1);
                }
            }

        }
        LOG.info("Found " + adjacencyList.size() + " edges and " + numEdges.get() + " edges.");
    }

    @Override
    public String getName() {
        return "graph distance metric";
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
        return getNeighbors(g, maxNeighbors, Integer.MAX_VALUE);
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
            seen.add(r.id);
            if (concepts == null || concepts.contains(r.id)) {
                result.add(new Neighbor(r.id, 1));
            }
        }

        for (int level = 2; !queue.isEmpty() && level <= maxSteps; level++) {
//            System.err.println("at level " + level + ", size is " + seen.size() + ", " + result.size());
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
                        seen.add(id2);
                        if (concepts == null || concepts.contains(id2)) {
                            result.add(new Neighbor(id2, level));
                        }
                    }
                    if (result.size() >= maxNeighbors) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public void setDirected(boolean directed) {
        this.directed = directed;
    }
}
