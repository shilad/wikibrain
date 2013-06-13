package org.wikapidia.core.dao.sql;

import org.apache.commons.io.IOUtils;
import org.jooq.SQLDialect;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.JooqUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public abstract class AbstractSqlDao {
    public static final Logger LOG = Logger.getLogger(LocalPageSqlDao.class.getName());

    protected final SQLDialect dialect;
    protected DataSource ds;
    protected SqlCache cache;

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

    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/local-page-schema.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    public  void useCache() throws DaoException {
        cache = new SqlCache(ds);
    }

    public void useCache(String dir) throws DaoException{
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
}
