package org.wikapidia.spatial.core.dao.postgis;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.spatial.core.SpatialLayer;
import org.wikapidia.spatial.core.SpatialReferenceSystem;
import org.wikapidia.spatial.core.SpatialUtils;
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
    private FastLoader fastLoader;

    public PostGISSpatialDataDao(PostGISDB db) throws DaoException{

        this.db = db;

    }


    @Override
    public TIntSet getAllGeomIdsInLayer(SpatialLayer sLayer) throws DaoException {
        return null;
    }

    @Override
    public TIntSet getAllGeomIdsInReferenceSystem(SpatialReferenceSystem srs) throws DaoException {
        return null;
    }

    @Override
    public TIntSet getAllGeomIdsInLayer(String layerName, String refSysName) throws DaoException {
        return null;
    }

    @Override
    public TIntSet getAllGeomIdsInReferenceSystem(String refSysName) throws DaoException {
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
    public TIntObjectHashMap<Geometry> getGeometriesForGeomIds(Collection<Integer> geomIds) throws DaoException {
        return null;
    }

    @Override
    public Integer getMaximumGeomId() throws DaoException {
        return null;
    }

    @Override
    public void beginSaveGeometries() throws DaoException {

        fastLoader = new FastLoader(db.getDataSource(), "geometries", new String[]{"ref_sys_name","layer_name","shape_type","geometry"});

    }

    @Override
    public void saveGeometry(Integer geomId, String layerName, String refSysName, Geometry g) throws DaoException {

        try{
            Object[] arr = new Object[]{
                refSysName,
                layerName,
                SpatialUtils.getShapeType(g),
                g.toText()};
            fastLoader.load(arr);
        }catch(WikapidiaException e){
            throw new DaoException(e);
        }



    }

    @Override
    public void endSaveGeometries() throws DaoException {

        fastLoader.endLoad();
        fastLoader.close();
        fastLoader = null;

    }
}
