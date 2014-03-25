package org.wikapidia.spatial.core.dao.postgis;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.dao.sql.WpDataSource;
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

        fastLoader = new FastLoader(db.getDataSource(), "geometries", new String[]{"ref_sys_name","layer_name","shape_type", "geom_id", "geometry"});

    }

    @Override
    public void saveGeometry(Integer geomId, String layerName, String refSysName, Geometry g) throws DaoException {

        try{
            Object[] arr = new Object[]{
                refSysName,
                layerName,
                SpatialUtils.getShapeType(g),
                geomId,
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

    public static class Provider extends org.wikapidia.conf.Provider<PostGISSpatialDataDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SpatialDataDao.class;
        }

        @Override
        public String getPath() {
            return "spatial.dao.spatialdata";
        }

        @Override
        public PostGISSpatialDataDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new PostGISSpatialDataDao(getConfigurator().get(PostGISDB.class));

            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
