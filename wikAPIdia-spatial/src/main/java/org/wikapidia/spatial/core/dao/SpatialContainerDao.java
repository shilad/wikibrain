package org.wikapidia.spatial.core.dao;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.SpatialContainer;
import org.wikapidia.spatial.core.SpatialContainerMetadata;
import org.wikapidia.spatial.core.SpatialLayer;
import org.wikapidia.spatial.core.SpatialReferenceSystem;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialContainerDao extends SpatialDao<SpatialContainer> {

    public Collection<Integer> getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException;

    public Collection<Integer> getAllGeomIdsInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public Set<Integer> getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException;

    public Set<Integer> getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException;

    public Collection<SpatialReferenceSystem> getAllSpatialReferenceSystems() throws DaoException;

    public Collection<SpatialLayer> getAllSpatialLayersInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public SpatialReferenceSystem getSpatialReferenceSystem(String rsName) throws DaoException;

    public SpatialLayer getSpatialLayer(String layerName, String rsName) throws DaoException;
}
