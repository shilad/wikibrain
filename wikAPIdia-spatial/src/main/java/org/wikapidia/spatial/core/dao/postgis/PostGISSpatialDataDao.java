package org.wikapidia.spatial.core.dao.postgis;

import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.SpatialLayer;
import org.wikapidia.spatial.core.SpatialReferenceSystem;
import org.wikapidia.spatial.core.dao.SpatialDataDao;

import java.sql.Statement;
import java.util.Collection;
import java.util.Map;

/**
 * Created by bjhecht on 12/30/13.
 */
public class PostGISSpatialDataDao implements SpatialDataDao {

    private final PostGISDB db;

    public PostGISSpatialDataDao(PostGISDB db) {
        this.db = db;
    }

    @Override
    public Iterable<Integer> getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException {
        return null;
    }

    @Override
    public Iterable<Integer> getAllGeomIdsInReferenceSystem(SpatialReferenceSystem srs) throws DaoException {
        return null;
    }

    @Override
    public Iterable<Integer> getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException {
        return null;
    }

    @Override
    public Iterable<Integer> getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException {
        return null;
    }

    @Override
    public Iterable<SpatialReferenceSystem> getAllSpatialReferenceSystems() throws DaoException {
        return null;
    }

    @Override
    public Iterable<SpatialLayer> getAllSpatialLayersInReferenceSystem(SpatialReferenceSystem srs) throws DaoException {
        return null;
    }

    @Override
    public Map<Integer, Geometry> getGeometriesForGeomIds(Collection<Integer> geomIds) throws DaoException {
        return null;
    }

    @Override
    public Integer getMaximumGeomId() throws DaoException {
        return null;
    }

    @Override
    public void saveGeometry(Integer geomId, String layerName, String refSysName, Geometry g) throws DaoException {

    }
}
