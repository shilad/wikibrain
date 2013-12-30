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

    public Iterable<Integer> getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException;

    public Iterable<Integer> getAllGeomIdsInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public Iterable<Integer> getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException;

    public Iterable<Integer> getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException;

    public Iterable<SpatialReferenceSystem> getAllSpatialReferenceSystems() throws DaoException;

    public Iterable<SpatialLayer> getAllSpatialLayersInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public SpatialReferenceSystem getSpatialReferenceSystem(String rsName) throws DaoException;

    public SpatialLayer getSpatialLayer(String layerName, String rsName) throws DaoException;
}
