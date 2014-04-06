package org.wikapidia.spatial.core.dao.postgis;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang.NotImplementedException;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.spatial.core.SpatialContainerMetadata;
import org.wikapidia.spatial.core.SpatialLayer;
import org.wikapidia.spatial.core.SpatialReferenceSystem;
import org.wikapidia.spatial.core.SpatialUtils;
import org.wikapidia.spatial.core.dao.SpatialDataDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
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
        //FastLoader(WpDataSource ds, String table, String[] fields)
        fastLoader = new FastLoader(db.getDataSource(), "geometries", new String[]{"item_id", "ref_sys_name","layer_name","geometry"});

    }


    /*
    Note: all implemented w/o prepared statements for thread safety reasons
     */
    @Override
    public Geometry getGeometry(int itemId, String layerName, String refSysName) throws DaoException {

        try {

            Statement s = this.db.getDataSource().getConnection().createStatement();
            String query = String.format("SELECT ST_AsBinary(geom) AS geom_bin" +
                    "FROM geometries WHERE item_id = %d AND layer_name = %s AND ref_sys_name = %s", itemId, layerName, refSysName);
            ResultSet r = s.executeQuery(query);
            List<Geometry> geoms = getBinaryGeometriesFromResultSet(r, "geom_bin");
            if (geoms.size() == 0) return null;
            return geoms.get(0);

        }catch(Exception e){
            throw new DaoException(e);
        }

    }

    private List<Geometry> getBinaryGeometriesFromResultSet(ResultSet r, String colName) throws SQLException, ParseException {

        List<Geometry> rVal = Lists.newArrayList();
        WKBReader wkbReader = new WKBReader();

        while(r.next()){
            byte[] wkb = r.getBytes(colName);
            Geometry g = wkbReader.read(wkb);
            rVal.add(g);
        }

        return rVal;

    }

    @Override
    public Iterable<Geometry> getGeometries(int itemId) throws DaoException {

        try {

            Statement s = this.db.getDataSource().getConnection().createStatement();
            String query = String.format("SELECT ST_AsBinary(geom) AS geom_bin" +
                    "FROM geometries WHERE item_id = %d", itemId);
            ResultSet r = s.executeQuery(query);
            return getBinaryGeometriesFromResultSet(r, "geom_bin");

        }catch(Exception e){
            throw new DaoException(e);
        }

    }

    @Override
    public Iterable<Integer> getAllItemsInLayer(String layerName, String refSysName) throws DaoException {
        return null;
    }

    @Override
    public Iterable<String> getAllRefSysNames() throws DaoException {

        try {

            Statement s = this.db.getDataSource().getConnection().createStatement();
            String query = String.format("SELECT DISTINCT ref_sys_name FROM geometries");
            ResultSet r = s.executeQuery(query);
            List<String> rVal = Lists.newArrayList();
            while(r.next()){
                rVal.add(r.getString("ref_sys_name"));
            }
            return rVal;


        }catch(SQLException e){
            throw new DaoException(e);
        }

    }

    @Override
    public Iterable<String> getAllLayerNames(String refSysName) throws DaoException {
        throw new DaoException(new NotImplementedException());
    }

    @Override
    public SpatialContainerMetadata getReferenceSystemMetadata(String refSysName) throws DaoException {
        return null;
    }

    @Override
    public SpatialContainerMetadata getLayerMetadata(String layerName, String refSysName) throws DaoException {
        return null;
    }

    @Override
    public void saveGeometry(int itemId, String layerName, String refSysName, Geometry g) throws DaoException {

        Object[] arr = new Object[]{
                itemId,
                layerName,
                refSysName,
                String.format("ST_GeomAsText(%s)", g.toText())};
        fastLoader.load(arr);

    }


    @Override
    public void beginSaveGeometries() throws DaoException {

        fastLoader = new FastLoader(db.getDataSource(), "geometries", new String[]{"item_id, layer_name, ref_sys_name, geom"});

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

        private static int numInstances = 0;
        @Override
        public PostGISSpatialDataDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("postgis")) {
                return null;
            }
            try {

                /*
                return new LocalLinkSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource"))
                 */

                System.out.println("GETTING DAO " + numInstances);
                numInstances++;
                WpDataSource wpDataSource = getConfigurator().get(WpDataSource.class,
                        "postgis");
                return new PostGISSpatialDataDao(new PostGISDB(wpDataSource));

            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
