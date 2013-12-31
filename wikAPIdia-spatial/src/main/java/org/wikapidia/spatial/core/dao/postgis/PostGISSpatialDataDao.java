package org.wikapidia.spatial.core.dao.postgis;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.SpatialLayer;
import org.wikapidia.spatial.core.SpatialReferenceSystem;
import org.wikapidia.spatial.core.dao.SpatialDataDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by bjhecht on 12/30/13.
 */
public class PostGISSpatialDataDao implements SpatialDataDao {

    private final PostGISDB db;
    private final PreparedStatement geomIdsInLayerPs;

    public PostGISSpatialDataDao(PostGISDB db) throws DaoException{

        this.db = db;
        geomIdsInLayerPs = db.advanced_prepareStatement("SELECT geom_id FROM geometries WHERE layer_name = ? and ref_sys_name = ?");


    }

    @Override
    public Iterable<Integer> getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException {

        try{

            Statement s = db.advanced_getStatement();
            geomIdsInLayerPs.setString(1, sLayer.getLayerName());
            geomIdsInLayerPs.setString(2, sLayer.getRefSysName());

            Set<Integer> rVal = Sets.newHashSet();
            ResultSet r = geomIdsInLayerPs.executeQuery();
            while (r.next()){
                rVal.add(r.getInt(1));
            }

            return rVal;


        }catch(SQLException e){

            throw new DaoException(e);

        }

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
