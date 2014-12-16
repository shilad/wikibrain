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
import org.wikibrain.spatial.util.ContainmentIndex;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class BorderingDistanceMetric implements SpatialDistanceMetric {
    private static final Logger LOG = Logger.getLogger(BorderingDistanceMetric.class.getName());

    /**
     * Default buffer width in degrees. This is about 0.1 kms.
     */
    private static final double DEFAULT_BUFFER_WIDTH = 0.001;

    private double bufferWidth = DEFAULT_BUFFER_WIDTH;

    private int maxSteps = 10;
    private final String layer;
    private final SpatialDataDao dao;
    private Map<Integer, Geometry> geometries;
    private TIntSet concepts;
    private final TIntObjectMap<TIntSet> adjacencyList = new TIntObjectHashMap<TIntSet>();
    private ContainmentIndex index;

    public BorderingDistanceMetric(SpatialDataDao dao, String layer) {
        this.dao = dao;
        this.layer = layer;
    }

    @Override
    public void setValidConcepts(TIntSet concepts) {
        this.concepts = concepts;
    }

    @Override
    public void enableCache(boolean enable) throws DaoException {
        final AtomicInteger numEdges = new AtomicInteger();
        geometries = dao.getAllGeometriesInLayer(layer, Precision.LatLonPrecision.HIGH);
        index = new ContainmentIndex();
        ParallelForEach.loop(geometries.keySet(), WpThreadUtils.getMaxThreads(),
                new Procedure<Integer>() {
                    @Override
                    public void call(Integer conceptId) throws Exception {
                        if (concepts != null && !concepts.contains(conceptId)) {
                            return;
                        }
                        TIntSet neighbors = getBorderingRegions(conceptId);
                        numEdges.addAndGet(neighbors.size());
                        synchronized (adjacencyList) {
                            adjacencyList.put(conceptId, neighbors);
                        }
                        index.insert(conceptId, geometries.get(conceptId));
                    }
                }, 50000);
        LOG.info("Found " + adjacencyList.size() + " nodes and " + numEdges.get() + " edges.");
    }

    private TIntSet getBorderingRegions(int conceptId) {
        Geometry bg = geometries.get(conceptId).buffer(bufferWidth);
        TIntSet borders = new TIntHashSet();
        for (int id : geometries.keySet()) {
            Geometry g2 = geometries.get(id);
            if (conceptId != id && bg.intersects(g2)) {
                borders.add(id);
            }
        }
        return borders;
    }

    @Override
    public String getName() {
        return "bordering distance metric";
    }

    @Override
    public double distance(Geometry g1, Geometry g2) {
        if (adjacencyList.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        int srcId = getContainingGeometry(g1);
        int destId = getContainingGeometry(g2);
        if (srcId < 0) {
            throw new IllegalArgumentException("No containing geometry for source geometry");
        }
        if (destId < 0) {
            throw new IllegalArgumentException("No containing geometry for destination geometry");
        }
        if (srcId == destId) {
            return 0;
        }

        TIntSet seen = new TIntHashSet();
        TIntLinkedList queue = new TIntLinkedList();
        for (int id: adjacencyList.get(srcId).toArray()) {
            if (id == destId) {
                return 1;
            }
            queue.add(id);
            seen.add(id);
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
                    if (id2 == destId) {
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
        if (adjacencyList.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        int srcId = getContainingGeometry(g);
        if (srcId < 0) {
            throw new IllegalArgumentException("No containing geometry for source geometry");
        }

        List<Neighbor> neighbors = new ArrayList<Neighbor>();
        neighbors.add(new Neighbor(srcId, 0));

        TIntSet added = new TIntHashSet();
        TIntSet seen = new TIntHashSet();
        TIntLinkedList queue = new TIntLinkedList();
        seen.add(srcId);
        added.add(srcId);
        queue.addAll(adjacencyList.get(srcId));

        int distance = 0;
        while (!queue.isEmpty() && neighbors.size() < maxNeighbors) {
            distance++;

            // Do all nodes at this level
            int nodes = queue.size();
            for (int i = 0; i < nodes; i++) {
                int id = queue.removeAt(0);
                if (!added.contains(id)) {
                    neighbors.add(new Neighbor(id, distance));
                    added.add(id);
                }
                if (!adjacencyList.containsKey(id)) {
                    continue;
                }
                for (int id2 : adjacencyList.get(id).toArray()) {
                    if (!seen.contains(id2)) {
                        queue.add(id2);
                        seen.add(id2);
                    }
                }
            }
        }
        return neighbors;
    }

    /**
     * Sets the buffer width in degrees for detecting neighbors who share a border.
     * @param bufferWidth
     */
    public void setBufferWidth(double bufferWidth) {
        this.bufferWidth = bufferWidth;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    private int getContainingGeometry(Geometry g) {
        if (index == null) {
            throw new IllegalStateException();
        }
        List<ContainmentIndex.Result> result = index.query(g);
//        System.err.println("query returned " + result.size());
        for (ContainmentIndex.Result r : result) {
            Geometry g2 = r.geometry;
            if (g == g2 || g.equals(g2)) {
                return r.id;
            }
            if (g2.contains(g)) {
                return r.id;
            }
        }
        return -1;
    }
}
