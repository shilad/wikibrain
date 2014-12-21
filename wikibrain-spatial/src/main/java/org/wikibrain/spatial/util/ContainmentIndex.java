package org.wikibrain.spatial.util;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
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
     * Default buffer width in degrees. This is about 0.1 kms.
     */
    private static final double DEFAULT_BUFFER_WIDTH = 0.001;

    private final TIntObjectMap<Geometry> geometries = new TIntObjectHashMap<Geometry>();
    private final SpatialIndex index = new STRtree();

    private double bufferWidth =  DEFAULT_BUFFER_WIDTH;

    public ContainmentIndex() {
    }

    public void insert(int id, Geometry geometry) {
        Envelope env = geometry.getEnvelopeInternal();
        env.expandBy(bufferWidth);
        synchronized (index) {
            index.insert(env, id);
            geometries.put(id, geometry);
        }
    }

    public List<Result> query(Geometry query) {
        Envelope env = query.getEnvelopeInternal();
        env.expandBy(bufferWidth);
        List<Result> results = new ArrayList<Result>();
        for (int id : (List<Integer>)index.query(env)) {
            Geometry g = geometries.get(id);
            if (g.contains(query)) {
                results.add(new Result(id, g));
            }
        }
        return results;
    }

    public void setBufferWidth(double bufferWidth) {
        this.bufferWidth = bufferWidth;
    }

    public static class Result {
        public final int id;
        public final Geometry geometry;

        public Result(int id, Geometry geometry) {
            this.id = id;
            this.geometry = geometry;
        }
    }
}
