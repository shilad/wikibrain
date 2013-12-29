package org.wikapidia.spatial.core.dao;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.SpatialContainerMetadata;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialMetadataDao {

    public Collection<Integer> getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException;

    public Collection<Integer> getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException;

    public SpatialContainerMetadata getLayerMetadata(String layerName, String refSysName) throws DaoException;

    public Map<String, SpatialContainerMetadata> getLayersMetadata(String refSysName) throws DaoException;

    public Collection<String> getAllReferenceSystemNames() throws DaoException;
}
