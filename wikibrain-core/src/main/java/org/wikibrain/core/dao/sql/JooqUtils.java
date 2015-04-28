package org.wikibrain.core.dao.sql;

import org.jooq.*;
import org.jooq.impl.DefaultConnectionProvider;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.core.dao.DaoException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.jooq.SQLDialect.*;

/**
 * This are in the JOOQ master, but have not yet been released.
 * Drop these and shift to JOOQ's builtins after the first post-3.0.0 release.
 */
public class JooqUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JooqUtils.class);

    /**
     * "Guess" the {@link SQLDialect} from a {@link Connection} instance.
     * <p>
     * This method tries to guess the <code>SQLDialect</code> of a connection
     * from the its connection URL as obtained by
     * {@link DatabaseMetaData#getURL()}. If the dialect cannot be guessed from
     * the URL (e.g. when using an JDBC-ODBC bridge), further actions may be
     * implemented in the future.
     *
     * @see #dialect(String)
     */
    @SuppressWarnings("deprecation")
    public static final SQLDialect dialect(Connection connection) {
        SQLDialect result = SQLDialect.SQL99;

        try {
            String url = connection.getMetaData().getURL();
            result = dialect(url);
        }
        catch (SQLException ignore) {}

        if (result == SQLDialect.SQL99) {
            // If the dialect cannot be guessed from the URL, take some other
            // measures, e.g. by querying DatabaseMetaData.getDatabaseProductName()
        }

        return result;
    }

    /**
     * "Guess" the {@link SQLDialect} from a connection URL.
     */
    @SuppressWarnings("deprecation")
    public static final SQLDialect dialect(String url) {

        // The below list might not be accurate or complete. Feel free to
        // contribute fixes related to new / different JDBC driver configuraitons
        if (url.startsWith("jdbc:cubrid:")) {
            return CUBRID;
        }
        else if (url.startsWith("jdbc:derby:")) {
            return DERBY;
        }
        else if (url.startsWith("jdbc:firebirdsql:")) {
            return FIREBIRD;
        }
        else if (url.startsWith("jdbc:h2:")) {
            return H2;
        }
        else if (url.startsWith("jdbc:hsqldb:")) {
            return HSQLDB;
        }
        else if (url.startsWith("jdbc:mysql:")
                || url.startsWith("jdbc:google:")) {
            return MYSQL;
        }
        else if (url.startsWith("jdbc:postgresql:")) {
            return POSTGRES;
        }
        else if (url.startsWith("jdbc:sqlite:")) {
            return SQLITE;
        }

        return SQLDialect.SQL99;
    }

    /**
     * Return the SQL connection associated with a DSLContext
     * @param context
     * @return the connection, or null.
     */
    public static Connection getConnection(DSLContext context) {
        ConnectionProvider provider = context.configuration().connectionProvider();
        if (provider instanceof DefaultConnectionProvider) {
            return ((DefaultConnectionProvider) provider).acquire();
        } else {
            return null;
        }
    }

    /**
     * Rollback the current transaction.
     * If a SQLException occurs while rolling back, it logs the error and returns false,
     * but does not rethrow the exception.
     *
     * @param context
     */
    public static boolean rollbackQuietly(DSLContext context) {
        if (context == null) {
            return false;
        }
        return WpDataSource.rollbackQuietly(getConnection(context));
    }

    public static void commit(DSLContext context) throws DaoException {
        try {
            getConnection(context).commit();
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }

    public static boolean tableExists(DSLContext context, Table table) {
        for (Table t : context.meta().getTables()) {
            if (t.getName().equalsIgnoreCase(table.getName())) {
                return true;
            }
        }
        return false;
    }
}
