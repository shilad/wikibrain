package org.wikapidia.core.dao.sql;

import org.jooq.SQLDialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.jooq.SQLDialect.*;

/**
 * This are in the JOOQ master, but have not yet been released.
 * Drop these and shift to JOOQ's builtins after the first post-3.0.0 release.
 */
public class JooqUtils {

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
        if (url.startsWith("jdbc:jtds:sybase:")) {
            return ASE;
        }
        else if (url.startsWith("jdbc:cubrid:")) {
            return CUBRID;
        }
        else if (url.startsWith("jdbc:db2:")) {
            return DB2;
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
        else if (url.startsWith("jdbc:ingres:")) {
            return INGRES;
        }
        else if (url.startsWith("jdbc:mysql:")
                || url.startsWith("jdbc:google:")) {
            return MYSQL;
        }
        else if (url.startsWith("jdbc:oracle:")
                || url.startsWith("jdbc:oracle:oci")) {
            return ORACLE;
        }
        else if (url.startsWith("jdbc:postgresql:")) {
            return POSTGRES;
        }
        else if (url.startsWith("jdbc:sqlite:")) {
            return SQLITE;
        }
        else if (url.startsWith("jdbc:sqlserver:")
                || url.startsWith("jdbc:jtds:sqlserver:")
                || url.startsWith("jdbc:microsoft:sqlserver:")
                || url.contains(":mssql")) {
            return SQLSERVER;
        }
        else if (url.startsWith("jdbc:sybase:")) {
            return SYBASE;
        }

        return SQLDialect.SQL99;
    }
}
