package org.wikibrain.spatial.dao;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.dao.DaoException;

import java.util.Map;
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
     * @throws org.wikibrain.core.dao.DaoException
     */
    public TIntSet getNeighboringItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, double minDist, double maxDist) throws DaoException;
    /**
     * Returns the item ids of the items spatially located with a given distance range with the geometry corresponding to the input (itemId, layerName, refSysName).
     * @param g The starting point
     * @param refSysName refSysName of the starting point
     * @param subLayers the layers in which to search for the objects (in the input ref sys)
     * @param minDist the min distance range in angular
     * @param maxDist the max distance range in angular
     * @return a IntSet contatins spatial items with in the given range in the given layer
     * @throws org.wikibrain.core.dao.DaoException
     */
    public TIntSet getNeighboringItemIds(Geometry g, String refSysName, Set<String> subLayers, double minDist, double maxDist) throws DaoException;
    /**
     * Returns the item ids of the items spatially located with a given distance range with the geometry corresponding to the input (itemId, layerName, refSysName).
     * @param itemId itemId of the starting point
     * @param layerName layer of the starting point
     * @param refSysName refSysName of the starting point
     * @param subLayers the layers in which to search for the objects (in the input ref sys)
     * @param maxDist the min distance range in km
     * @return a IntSet contatins spatial items with in the given range in the given layer
     * @throws org.wikibrain.core.dao.DaoException
     */
    public TIntSet getMaxDistanceKmItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, double maxDist) throws DaoException;
    /**
     * Returns the item ids of the items spatially located with a given distance range with the geometry corresponding to the input (itemId, layerName, refSysName).
     * @param g The starting point
     * @param refSysName refSysName of the starting point
     * @param subLayers the layers in which to search for the objects (in the input ref sys)
     * @param maxDist the min distance range in km
     * @return a IntSet contatins spatial items with in the given range in the given layer
     * @throws org.wikibrain.core.dao.DaoException
     */
    public TIntSet getMaxDistanceKmItemIds(Geometry g, String refSysName, Set<String> subLayers, double maxDist) throws DaoException;


    public Map<Integer, Geometry> getKNNeighbors(Integer itemId, int k, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException;

    public Map<Integer, Geometry> getKNNeighbors(Geometry geometry, int k, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException;

    public Map<Integer, Geometry> getNeighbors(Geometry geometry, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException;

    public Map<Integer, Geometry> getNeighbors(Integer itemId, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException;



}
