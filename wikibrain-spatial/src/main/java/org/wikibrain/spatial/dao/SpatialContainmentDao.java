package org.wikibrain.spatial.dao;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;

import java.util.Set;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialContainmentDao {

    public static enum ContainmentOperationType {CONTAINMENT, INTERSECTION};


    /**
     * Returns the item ids of the items spatially contained/intersected with the geometry corresponding to the input (itemId, layerName, refSysName).
     * @param itemId itemId of the container
     * @param layerName layer of the container
     * @param refSysName refSysName of the container
     * @param subLayers the layers in which to search for contained objects (in the input ref sys)
     * @param opType whether the function should use a contains() or intersects() operation
     * @return
     * @throws DaoException
     */
    public TIntSet getContainedItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, ContainmentOperationType opType) throws DaoException;


    /**
     * Returns the item ids spatially contained/intersected with the input geometry
     * @param g
     * @param refSysName the ref sys in which to look for contained items
     * @param subLayers the layers in which to look for contained items
     * @param opType whether the function should use a contains() or intersects() operation
     * @return
     * @throws DaoException
     */
    public TIntSet getContainedItemIds(Geometry g, String refSysName, Set<String> subLayers, ContainmentOperationType opType) throws DaoException;



}
