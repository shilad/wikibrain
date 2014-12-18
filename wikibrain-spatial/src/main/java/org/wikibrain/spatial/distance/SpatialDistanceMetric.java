package org.wikibrain.spatial.distance;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;

import java.util.List;

/**
 *
 * TODO: add versions of the methods for local and universal ids
 *
 * @author Shilad Sen
 */
public interface SpatialDistanceMetric {
    public static class Neighbor implements Comparable<Neighbor> {
        public final int conceptId;
        public final double distance;

        public Neighbor(int conceptId, double distance) {
            this.conceptId = conceptId;
            this.distance = distance;
        }

        @Override
        public int compareTo(Neighbor that) {
            if (this.distance < that.distance) {
                return -1;
            } else if (this.distance > that.distance) {
                return +1;
            } else {
                return 0;
            }
        }
    }

    void setValidConcepts(TIntSet concepts);

    /**
     * Build an efficient in-memory cache if helpful.
     */
    public void enableCache(boolean enable) throws DaoException;

    /**
     * Describes the spatial distance metric.
     *
     * @return
     */
    public String getName();

    /**
     * Calculates the distance between two geometries.
     *  @param g1
     * @param g2
     */
    public double distance(Geometry g1, Geometry g2);

    /**
     * Returns the distance matrix between the specified geometries.
     */
    public float[][] distance(List<Geometry> rowGeometries, List<Geometry> colGeometries);

    /**
     * Returns the distance matrix between the specified geometries.
     */
    public float[][] distance(List<Geometry> geometries);

    /**
     * Returns the closest points to a particular geometry.
     * @param g
     * @param maxNeighbors
     * @return
     */
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors);

    /**
     * Returns the closest points to a particular geometry, thresholded at some cutoff.
     * @param g
     * @param maxNeighbors
     * @param maxDistance
     * @return
     */
    public List<Neighbor> getNeighbors(Geometry g, int maxNeighbors, double maxDistance);

}
