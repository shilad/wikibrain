package org.wikibrain.spatial.util;

import ags.utils.dataStructures.MaxHeap;
import ags.utils.dataStructures.trees.thirdGenKD.KdTree;
import ags.utils.dataStructures.trees.thirdGenKD.SquareEuclideanDistanceFunction;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.GeodeticCalculator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a spherical approximation of the closest points to a particular point.
 * This class is threadsafe for multiple readers or writers (but not both).
 *
 *
 * @author Shilad Sen
 */
public class ClosestPointIndex implements Serializable {
    private final KdTree<Result> index = new KdTree<Result>(3);

    public ClosestPointIndex() {}

    /**
     * Insert a geometry into the index and associate it with a particular id.
     * If the geometry is not a point, uses WikiBrainSpatialUtils.getCenter to
     * convert it to a single point.
     *
     * @param id
     * @param geometry
     */
    public void insert(int id, Geometry geometry) {
        Point p = WikiBrainSpatialUtils.getCenter(geometry);
        Result r = new Result(id, geometry, p);
        synchronized (index) {
            index.addPoint(WikiBrainSpatialUtils.get3DPoints(p), r);
        }
    }

    /**
     * Return the closest points to the specified point.
     * The returned distances are estimated using the Haversine formula.
     * @param query
     * @param maxNeighbors
     * @return
     */
    public List<Result> query(Geometry query, int maxNeighbors) {
        final Point c = WikiBrainSpatialUtils.getCenter(query);
        final double[] c1 = WikiBrainSpatialUtils.get3DPoints(c);
        MaxHeap<Result> heap = index.findNearestNeighbors(
                c1,
                maxNeighbors,
                new SquareEuclideanDistanceFunction());

        List<Result> results = new ArrayList<Result>();
        while (heap.size() > 0) {
            Result r = heap.getMax();
            heap.removeMax();
            double d = WikiBrainSpatialUtils.haversine(c, r.point);
            results.add(new Result(r.id, r.geometry, r.point, d));
        }
        Collections.reverse(results);

        return results;
    }

    public int size() {
        return index.size();
    }

    public static class Result implements Serializable {
        public final int id;
        public final Geometry geometry;
        public final Point point;
        public final double distance;

        public Result(int id, Geometry geometry, Point point) {
            this(id, geometry, point, -1);
        }

        public Result(int id, Geometry geometry, Point point, double distance) {
            this.id = id;
            this.geometry = geometry;
            this.point = point;
            this.distance = distance;
        }
    }
}
