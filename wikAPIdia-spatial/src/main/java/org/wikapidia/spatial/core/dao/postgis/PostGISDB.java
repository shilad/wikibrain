package org.wikapidia.spatial.core.dao.postgis;

import org.wikapidia.core.dao.DaoException;

import java.sql.*;
import java.util.Properties;

/**
 * Created by bjhecht on 12/30/13.
 */
public class PostGISDB {

    private Connection c;

    public PostGISDB(String host, String databaseName, String userName, String password) throws DaoException {
        this.c = getConnection(host, databaseName, userName, password);
        if (needsToBeInitialized()) initializeDatabase();
    }

    private Connection getConnection(String host, String databaseName, String userName, String password) throws DaoException{

        try {

            Class.forName("org.postgresql.Driver").newInstance();
            String url = "jdbc:postgresql://" + host + "/"+ databaseName;
            Properties props = new Properties();
            props.setProperty("user", userName);
            props.setProperty("password", password);

            Connection rVal;
            try{
                rVal = (Connection) DriverManager.getConnection(url, props);
            }catch (SQLException e){ // if it doesn't work at 5432, do 5433
                url = "jdbc:postgresql://" + host + ":5432/"+ databaseName;
                rVal = (Connection) DriverManager.getConnection(url, props);
            }
            ((org.postgresql.PGConnection)rVal).addDataType("geometry", org.postgis.PGgeometry.class);
            return rVal;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            throw new DaoException("There was an error initializing the connection with the PostGIS database.", e);
        }
    }

    public Statement advanced_getStatement() throws DaoException{

        try{
            return c.createStatement();
        }catch(SQLException e){
            throw new DaoException(e);
        }

    }

    public PreparedStatement advanced_prepareStatement(String psSql) throws DaoException{
        return advanced_prepareStatement(psSql, false);
    }


    public PreparedStatement advanced_prepareStatement(String psSql, boolean scrollable) throws DaoException{

        try{
            if (scrollable){
                return c.prepareStatement(psSql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            }else{
                return c.prepareStatement(psSql);
            }
        }catch(SQLException e){
            throw new DaoException(e);
        }

    }

    private boolean needsToBeInitialized() throws DaoException{

        try{
            DatabaseMetaData md = c.getMetaData();
            ResultSet rs = md.getTables(null, null, "wapi_geometries", null);
            rs.first();
            return (rs.getRow() < 1);
        }catch(SQLException e){
            throw new DaoException(e);
        }

    }

    public static final String layerNameType = "VARCHAR(63) NOT NULL";
    public static final String refSysNameType = "VARCHAR(63) NOT NULL";
    public static final String geomIdType = "INTEGER NOT NULL";
    public static final String shapeTypeType = "SMALLINT NOT NULL";

    private void initializeDatabase() throws DaoException{

        try{

            Statement s = advanced_getStatement();

            // geometries
            String spatialObjectsSql = "CREATE TABLE geometries (ref_sys_name "+refSysNameType+" NOT NULL," +
                    " layer_name "+layerNameType+" NOT NULL, " +
                    " shape_type "+shapeTypeType+"," +
                    " geom_id "+geomIdType+" PRIMARY KEY)";
            System.out.println(spatialObjectsSql);
            s.execute(spatialObjectsSql);
            s.execute("CREATE INDEX rs_layer_type ON geometries (ref_sys_name, layer_name, shape_type)");
            s.execute("SELECT AddGeometryColumn('public','geometries','geometry',-1,'GEOMETRY',2)");
            s.execute("CREATE INDEX geometry_index ON geometries USING GIST ( geometry )");

            // spatiotags
            String spatiotagsSqlF = "CREATE TABLE spatiotags (local_id INTEGER NOT NULL, lang_id SMALLINT NOT NULL, geom_id + " + geomIdType + " PRIMARY KEY)";
            s.execute(spatiotagsSqlF);
            s.execute("CREATE INDEX geom_lookup ON spatiotags (local_id, lang_id)");

            s.close();

        }catch(SQLException e){
            throw new DaoException(e);
        }

    }


}
