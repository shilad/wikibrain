package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.dao.DaoException;

import java.util.Set;

/**
 * Created by toby on 4/17/14.
 */
public interface SpatialNeighborDao {
    /**
     * Returns the item ids of the items spatially located with a given distance range with the geometry corresponding to the input (itemId, layerName, refSysName).
     * @param itemId itemId of the container
     * @param layerName layer of the container
     * @param refSysName refSysName of the container
     * @param subLayers the layers in which to search for contained objects (in the input ref sys)
     * @param minDist the min distance range
     * @param maxDist the max distance range
     * @return
     * @throws org.wikapidia.core.dao.DaoException
     */
    public TIntSet getNeighboringItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, double minDist, double maxDist) throws DaoException;

    public TIntSet getNeighboringItemIds(Geometry g, String refSysName, Set<String> subLayers, double minDist, double maxDist) throws DaoException;

}
