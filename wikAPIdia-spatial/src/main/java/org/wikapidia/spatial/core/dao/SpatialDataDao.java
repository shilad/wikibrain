package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
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
public interface SpatialDataDao {

    public TIntSet getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException;

    public TIntSet getAllGeomIdsInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public TIntSet getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException;

    public TIntSet getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException;

    public Iterable<SpatialReferenceSystem> getAllSpatialReferenceSystems() throws DaoException;

    public Iterable<SpatialLayer> getAllSpatialLayersInReferenceSystem(SpatialReferenceSystem srs) throws DaoException;

    public TIntObjectHashMap<Geometry> getGeometriesForGeomIds(Collection<Integer> geomIds) throws DaoException;

    public Integer getMaximumGeomId() throws DaoException;

    public void saveGeometry(Integer geomId, String layerName, String refSysName, Geometry g) throws DaoException;

}
