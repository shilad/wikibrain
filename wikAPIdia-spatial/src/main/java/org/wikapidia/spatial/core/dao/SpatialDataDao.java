package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.SpatialContainerMetadata;

import java.util.Map;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialDataDao {


    public Geometry getGeometry(int itemId, String layerName, String refSysName) throws DaoException;

    public Map<Integer, Geometry> getAllGeometries(String layerName, String refSysName) throws DaoException;

    public Iterable<Geometry> getGeometries(int itemId) throws DaoException;

    public Iterable<Integer> getAllItemsInLayer(String layerName, String refSysName) throws DaoException;

    public Iterable<String> getAllRefSysNames() throws DaoException;

    public Iterable<String> getAllLayerNames(String refSysName) throws DaoException;

    public SpatialContainerMetadata getReferenceSystemMetadata(String refSysName) throws DaoException;

    public SpatialContainerMetadata getLayerMetadata(String layerName, String refSysName) throws DaoException;

    public void beginSaveGeometries() throws DaoException;

    public void endSaveGeometries() throws DaoException;

    public void saveGeometry(int itemId, String layerName, String refSysName, Geometry g) throws DaoException;





    /*public TIntSet getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException;

    public TIntSet getAllGeomIdsInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public TIntSet getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException;

    public TIntSet getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException;

    public Iterable<SpatialReferenceSystem> getAllSpatialReferenceSystems() throws DaoException;

    public Iterable<SpatialLayer> getAllSpatialLayersInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public TIntObjectHashMap<Geometry> getGeometriesForGeomIds(Collection<Integer> geomIds) throws DaoException;

    public Integer getMaximumGeomId() throws DaoException;

    public void beginSaveGeometries() throws DaoException;

    public void saveGeometry(Integer geomId, String layerName, String refSysName, Geometry g) throws DaoException;

    public void endSaveGeometries() throws DaoException;*/

}
