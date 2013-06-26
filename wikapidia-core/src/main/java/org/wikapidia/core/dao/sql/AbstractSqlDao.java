package org.wikapidia.core.dao.sql;

import org.jooq.SQLDialect;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.JooqUtils;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ari Weiland
 *
 * A SQL Dao superclass that contains a few important parameters and utility methods
 * ubiquitous to all SQL Daos.
 */
public abstract class AbstractSqlDao {
    public static final Logger LOG = Logger.getLogger(LocalPageSqlDao.class.getName());
    public static final int DEFAULT_FETCH_SIZE = 1000;

    protected final SQLDialect dialect;
    protected DataSource ds;
    protected SqlCache cache;
    private int fetchSize = DEFAULT_FETCH_SIZE;

    public AbstractSqlDao(DataSource dataSource) throws DaoException {
        ds = dataSource;
        Connection conn = null;
        try {
            conn = ds.getConnection();
            this.dialect = JooqUtils.dialect(conn);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
        cache = null;
    }

    public void useCache(File dir) throws DaoException{
        cache = new SqlCache(ds, dir);
    }

    /**
     * Close a connection without generating an exception if it fails.
     * @param conn
     */
    public static void quietlyCloseConn(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to close connection: ", e);
            }
        }
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }
}
