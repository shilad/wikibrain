package org.wikibrain.spatial.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class ContainmentIndex implements Serializable {
    /**
     * Default buffer width 1 in degrees. These are about 0.1 kms and 10 km.
     */
    private static final double [] DEFAULT_BUFFER_WIDTHS = new double[] { 0.0, 0.001, 0.1 };


    private final TIntObjectMap<Geometry> geometries = new TIntObjectHashMap<Geometry>();
    private TIntObjectMap<Geometry> [] expanded;

    private STRtree[] indexes;
    private double [] bufferWidths;
    private boolean forceContains = false;

    public ContainmentIndex() {
        setBufferWidths(DEFAULT_BUFFER_WIDTHS);
    }

    public void insert(int id, Geometry geometry) {
        for (int i = 0; i < bufferWidths.length; i++) {
            Geometry exp = geometry.buffer(bufferWidths[i]);
            Envelope env = exp.getEnvelopeInternal();
            synchronized (indexes[i]) {
                indexes[i].insert(env, id);
                expanded[i].put(id, exp);
            }
            synchronized (geometries) {
                geometries.put(id, geometry);
            }
        }
    }

    public List<Result> getContainer(Geometry query) {
        List<Result> results = new ArrayList<Result>();
        for (int i = 0; i < bufferWidths.length; i++) {
            Envelope env = new Envelope(query.getEnvelopeInternal());
            for (int id : (List<Integer>)indexes[i].query(env)) {
                Geometry g = expanded[i].get(id);
                if (g.contains(query)) {
                    results.add(new Result(id, g));
                }
            }
            if (!results.isEmpty()) break;
        }
        if (results.isEmpty() && forceContains) {
            double closestDistance = 0;
            int closestId = -1;
            for (int id : geometries.keys()) {
                Coordinate[] pair = DistanceOp.nearestPoints(query, geometries.get(id));
                double d = pair[0].distance(pair[1]);
                if (closestId < 0 || d < closestDistance) {
                    closestDistance = d;
                    closestId = id;
                }
            }
            if (closestId >= 0) {
                results.add(new Result(closestId, geometries.get(closestId)));
            }
        }
        return results;
    }

    public void setBufferWidths(double [] bufferWidths) {
        this.bufferWidths = bufferWidths;
        this.indexes = new STRtree[this.bufferWidths.length];
        this.expanded = new TIntObjectHashMap[this.bufferWidths.length];
        for (int i = 0; i < bufferWidths.length; i++) {
            this.indexes[i] = new STRtree();
            this.expanded[i] = new TIntObjectHashMap<Geometry>();
        }
    }

    public static class Result {
        public final int id;
        public final Geometry geometry;

        public Result(int id, Geometry geometry) {
            this.id = id;
            this.geometry = geometry;
        }
    }

    public void setForceContains(boolean forceContains) {
        this.forceContains = forceContains;
    }
}
