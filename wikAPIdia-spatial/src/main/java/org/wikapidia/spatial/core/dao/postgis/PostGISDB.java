package org.wikapidia.spatial.core.dao.postgis;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.spatial.core.SpatialLayer;
import org.wikapidia.spatial.core.SpatialReferenceSystem;
import org.wikapidia.spatial.core.dao.SpatialDataDao;

import java.sql.*;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Brent Hecht on 12/30/13.
 */
public class PostGISDB{

    private WpDataSource wpDataSource;
    private FastLoader geometryFastLoader = null;
    private FastLoader spatiotagFastLoader = null;

    public PostGISDB(WpDataSource wpDataSource) throws DaoException {

        this.wpDataSource = wpDataSource;
        if (needsToBeInitialized()) wpDataSource.executeSqlResource("db/postgis-db.schema.sql");
    }

    public WpDataSource getDataSource(){
        return wpDataSource;
    }

    private boolean needsToBeInitialized() throws DaoException{

        try{

            Connection c = wpDataSource.getConnection();
            DatabaseMetaData md = wpDataSource.getConnection().getMetaData();
            ResultSet rs = md.getTables(null, null, "geometries", null);
            rs.first();
            boolean rVal = (rs.getRow() < 1);
            c.close();
            return rVal;

        }catch(SQLException e){
            throw new DaoException(e);
        }

    }



    public static class Provider extends org.wikapidia.conf.Provider<PostGISDB> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PostGISDB.class;
        }

        @Override
        public String getPath() {
            return "spatial.dao.postgis";
        }

        @Override
        public PostGISDB get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {

            try {
                WpDataSource wpDataSource = getConfigurator().get(
                        WpDataSource.class,
                        config.getString("pgisdatasource"));
                return new PostGISDB(wpDataSource);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }


}
