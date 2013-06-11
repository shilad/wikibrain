package org.wikapidia.core.dao.sql;

import org.jooq.SQLDialect;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.JooqUtils;

import javax.sql.DataSource;
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
