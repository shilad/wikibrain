package org.wikapidia.spatial.core.dao.postgis;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.dao.sql.WpDataSource;

import java.sql.*;

/**
 * Created by Brent Hecht on 12/30/13.
 */
public class PostGISDB{

    private WpDataSource wpDataSource;
    private FastLoader geometryFastLoader = null;
    private FastLoader spatiotagFastLoader = null;

    public PostGISDB(WpDataSource wpDataSource) throws DaoException {
        this.wpDataSource = wpDataSource;
        if (needsToBeInitialized()) {
            wpDataSource.executeSqlResource("/db/postgis-db.nonspatial.sql");

            try {
                Connection c = wpDataSource.getConnection();
                Statement s = c.createStatement();
                s.execute("SELECT AddGeometryColumn('geometries','geometry',-1,'GEOMETRY',2)"); //TODO: This should be in one of the .sql files, but the PostGIS functions couldn't find the 'public' schema for some reason
                s.execute("CREATE INDEX geometry_index ON geometries USING GIST ( geometry )");
                c.commit();
                s.close();
                c.close();
            }catch(SQLException e){
                throw new DaoException(e);
            }

        }
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
//            c.close();
            return rVal;

        }catch(SQLException e){
            throw new DaoException(e);
        }

    }


//
//    public static class Provider extends org.wikapidia.conf.Provider<PostGISDB> {
//        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
//            super(configurator, config);
//        }
//
//        @Override
//        public Class getType() {
//            return PostGISDB.class;
//        }
//
//        @Override
//        public String getPath() {
//            return "dao.dataSource.postgis";
//        }
//
//        @Override
//        public PostGISDB get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
//
//            try {
//                WpDataSource wpDataSource = getConfigurator().get(
//                        WpDataSource.class,
//                        config.getString("pgisdatasource"));
//                return new PostGISDB(wpDataSource);
//            } catch (DaoException e) {
//                throw new ConfigurationException(e);
//            }
//        }
//    }
//

}
