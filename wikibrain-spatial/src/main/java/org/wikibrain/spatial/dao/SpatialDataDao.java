package org.wikibrain.spatial.dao;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.SpatialContainerMetadata;
import org.wikibrain.spatial.constants.Precision;

import java.util.List;
import java.util.Map;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialDataDao {

    /**
     * Gets a geometry by Wikidata item id, layer name, and reference system name.
     * @param itemId
     * @param layerName
     * @param refSysName
     * @return
     * @throws DaoException
     */
    public Geometry getGeometry(int itemId, String layerName, String refSysName) throws DaoException;

    /**
     * Gets a geometry by Wikidata item id and layer name. Assumes "earth" reference system.
     * @param itemId
     * @param layerName
     * @return
     * @throws DaoException
     */
    public Geometry getGeometry(int itemId, String layerName) throws DaoException;

    /**
     * Gets a geometry by Wikidata item id, layer name, and minimum precision. Assumes "earth" reference system.
     * All geometries of shape type greater than point are high precision.
     * @param itemId
     * @param layerName
     * @param minPrecision See definition of LatLonPrecision
     * @return
     * @throws DaoException
     */
    public Geometry getGeometry(int itemId, String layerName, Precision.LatLonPrecision minPrecision) throws DaoException;

    /**
     * Gets a geometry by article name, language, and layer. Assumes "earth" reference system.
     * @param articleName (e.g. "Minnesota", "Minneapolis", "Kalifornien")
     * @param language (e.g. Language.EN, Language.DE)
     * @param layerName (e.g. Layers.STATE)
     * @return the geometry, or null if no geometry could be found
     * @throws DaoException
     */
    public Geometry getGeometry(String articleName, Language language, String layerName) throws DaoException;

    /**
     * Gets a geometry by article name, language, layer, and minimum precision. Assumes "earth" reference system.
     * All geometries of shape type greater than point are high precision.
     * @param articleName
     * @param language
     * @param layerName
     * @param minPrecision See definition of LatLonPrecision
     * @return
     * @throws DaoException
     */
    public Geometry getGeometry(String articleName, Language language, String layerName, Precision.LatLonPrecision minPrecision) throws DaoException;

    /**
     * public Geometry getGeometry(String articleName, Language language, String layerName) throws DaoException;
     * @param articleName (e.g. "Minnesota", "Minneapolis", "Kalifornien")
     * @param language (e.g. Language.EN, Language.DE)
     * @param layerName (e.g. Layers.STATE)
     * @param refSysName (e.g. Layers.EARTH)
     * @return the geometry, or null if no geometry could be found
     * @throws DaoException
     */

    // getGeometry("Minnesota", Language.SIMPLE, "gadm1", "earth");
    public Geometry getGeometry(String articleName, Language language, String layerName, String refSysName) throws DaoException;

    /**
     * Gets all geometries associated with a given Wikidata item id (all layers, all reference systems)
     * @param itemId
     * @return
     * @throws DaoException
     */
    public Map<String, Geometry> getGeometries(int itemId) throws DaoException;



    /**
     * Gets all the geometries in a given layer.
     * @param layerName
     * @param refSysName
     * @return null if layer does not exist
     * @throws DaoException
     */
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, String refSysName) throws DaoException;


    /**
     * Gets all the geometries in a given layer, assumes 'earth' reference system
     * @param layerName
     * @return null if layer does not exist
     * @throws DaoException
     */
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName) throws DaoException;


    /**
     * Gets all the geometries in a given layer with a minimum precision, assumes 'earth' reference system
     * @param layerName
     * @param minPrecision See definition of LatLonPrecision
     * @return not if layer does not exist
     * @throws DaoException
     */
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, Precision.LatLonPrecision minPrecision) throws DaoException;


    /**
     * Gets all geometries in layerName that are not in notInLayers
     * @param layerName
     * @param notInLayers
     * @param refSysName the reference system for both layerName and notInLayers
     * @return null if layer does not exist
     * @throws DaoException
     */
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, String[] notInLayers, String refSysName) throws DaoException;


    /**
     *
     * @param idList an iterable of ids of geometries
     * @param layerName
     * @param refSysName the reference system for both layerName and notInLayers
     * @return null if layer does not exist
     * @throws DaoException
     */
    public Map<Integer, Geometry> getBulkGeometriesInLayer(List<Integer> idList, String layerName, String refSysName) throws DaoException;

    /**
     * Gets the names of all loaded reference systems.
     * @return
     * @throws DaoException
     */
    public Iterable<String> getAllRefSysNames() throws DaoException;

    /**
     * Gets the names of all loaded layers.
     * @param refSysName
     * @return
     * @throws DaoException
     */
    public Iterable<String> getAllLayerNames(String refSysName) throws DaoException;

    /**
     * Gets the metadata for a given reference system.
     * @param refSysName
     * @return
     * @throws DaoException
     */
    public SpatialContainerMetadata getReferenceSystemMetadata(String refSysName) throws DaoException;

    /**
     * Gets the metadata for a given layer
     * @param layerName
     * @param refSysName
     * @return
     * @throws DaoException
     */
    public SpatialContainerMetadata getLayerMetadata(String layerName, String refSysName) throws DaoException;

    /**
     * This should be called prior to any saveGeometry() calls.
     * @throws DaoException
     */
    public void beginSaveGeometries() throws DaoException;

    /**
     * This should be called at the end of a spatial data loading process (when all the saveGeometry() calls are completed).
     * Usually will contain indexing and related functionality.
     * @throws DaoException
     */
    public void endSaveGeometries() throws DaoException;

    /**
     * Saves a geometry. Should only occur during a loading process (advanced only).
     * @param itemId
     * @param layerName
     * @param refSysName
     * @param g
     * @throws DaoException
     */
    public void saveGeometry(int itemId, String layerName, String refSysName, Geometry g) throws DaoException;


    /**
     * Removes the layer with the given reference system.
     * @param refSysName
     * @param layerName
     * @throws DaoException
     */
    public void removeLayer(String refSysName, String layerName) throws DaoException;

    /**
     * Optimizes the database, if necessary.
     */
    public void optimize() throws DaoException;
}
