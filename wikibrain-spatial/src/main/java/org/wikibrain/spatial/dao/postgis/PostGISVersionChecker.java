package org.wikibrain.spatial.dao.postgis;

import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;
import org.wikibrain.core.dao.DaoException;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class PostGISVersionChecker extends PostgisNGDataStoreFactory {
    public static Logger LOG = LoggerFactory.getLogger(PostGISVersionChecker.class);

    public void verifyVersion(Map params) throws DaoException {
        JDBCDataStore dataStore = new JDBCDataStore();
        SQLDialect dialect = createSQLDialect(dataStore);
        dataStore.setSQLDialect(dialect);
        DataSource ds;
        try {
            ds = super.createDataSource(params, dialect);
        } catch (IOException e) {
            throw new DaoException(e);
        }

        LOG.info("checking for postgis extension");

        // First pass through loop may install extension. Second pass should work!
        for (int i = 0; i < 2; i++) {
            if (checkAndInstall(ds)) {
                return;
            }
        }
        throw new DaoException("Failed to create PostGIS extension for database. Is PostGIS 2.x installed?");

    }

    private boolean checkAndInstall(DataSource ds) throws DaoException {
        JDBCDataStore closer = new JDBCDataStore();
        // check we have postgis
        Connection cnx = null;
        try {
            cnx = ds.getConnection();
        } catch (SQLException e) {
            throw new DaoException(e);
        }
        Statement st = null;
        try {
            st = cnx.createStatement();
        } catch (SQLException e) {
            closer.closeSafe(cnx);
            throw new DaoException(e);
        }
        try {
            ResultSet rs = st.executeQuery("select PostGIS_version()");
            rs.next();
            String version = rs.getString(1).trim();
            LOG.info("Found PostGIS version " + version);
            closer.closeSafe(rs);
            closer.closeSafe(st);
            closer.closeSafe(cnx);
            if (version.startsWith("2.")) {
                return true;
            } else {
                throw new DaoException("Invalid PostGIS version: " + version + ". Wikibrain requires 2.x");
            }
        } catch (SQLException e) {
            // Extension not available. Try to create it.
        }
        LOG.info("PostGIS extension not available for the database. Trying to create it.");

        try {
            st.execute("create extension postgis");
        } catch (SQLException e) {
            throw new DaoException("Failed to create PostGIS extension for database. Is PostGIS 2.x installed?");
        } finally {
            closer.closeSafe(st);
            closer.closeSafe(cnx);
        }
        return false;
    }
}
