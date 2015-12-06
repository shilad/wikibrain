package org.wikibrain.core.dao.sql;

import com.jolbox.bonecp.BoneCPDataSource;
import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.Provider;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.utils.WpThreadUtils;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class WpDataSource implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(WpDataSource.class);

    private DataSource dataSource;
    private Settings settings;
    private SQLDialect dialect;

    public WpDataSource(DataSource dataSource) throws DaoException {
        this.settings = new Settings();
        this.dataSource = dataSource;
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            this.dialect = JooqUtils.dialect(conn);
        } catch (SQLException e) {
            throw new DaoException("SQL Dao Failed. Check if the table exists / if the desired information has been parsed and stored in the database\n" + e.toString());
        } finally {
            closeQuietly(conn);
        }
        // Postgres uses a lowercase "public" main schema
        if (this.dialect == SQLDialect.POSTGRES) {
            settings.setRenderNameStyle(RenderNameStyle.LOWER);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
            // Since we're bulk loading, dirty reads are fine. I think....
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
        return conn;
    }

    /**
     * Rollback the current transaction.
     * If a SQLException occurs while rolling back, it logs the error and returns false,
     * but does not rethrow the exception.
     *
     * @param conn
     */
    public static boolean rollbackQuietly(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            conn.rollback();
            return true;
        } catch (SQLException e) {
            LOG.error("rollback failed: ", e);
            return false;
        }
    }
    public static boolean rollbackQuietly(DSLContext context) {
        if (context == null) {
            return false;
        }
        return JooqUtils.rollbackQuietly(context);
    }

    public DSLContext getJooq() throws DaoException {
        try {
            return DSL.using(getConnection(), dialect, settings);
        } catch (SQLException e) {
            throw new DaoException("SQL Dao Failed. Check if the table exists / if the desired information has been parsed and stored in the database\n" + e.toString());
        }
    }

    public void freeJooq(DSLContext context) {
        Connection conn = JooqUtils.getConnection(context);
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            closeQuietly(conn);
        }
    }

    /**
     * Executes a sql resource on the classpath
     * @param name Resource path - e.g. "/db/local-page.schema.sql"
     * @throws DaoException
     */
    public void executeSqlResource(String name) throws DaoException {
        String script = null;
        try {
//            System.out.println(name);
            script = IOUtils.toString(AbstractSqlDao.class.getResource(name));
        } catch (IOException e) {
            throw new DaoException(e);
        }
        script = translateSqlScript(script);
        Connection conn=null;
        try {
            conn = getConnection();
            for (String s : script.split(";")) {
                if (s.replaceAll(";", "").trim().isEmpty()) {
                    continue;
                }
                LOG.debug("executing:\n" + s + "\n=========================================\n");



                Statement st = conn.createStatement();
                //ResultSet rs = st.executeQuery("SHOW search_path");
                //rs.next();
                //System.out.println(rs.getString(1));
//                System.out.println(s);
                st.execute(s + ";");
                st.close();

            }
            conn.commit();
        } catch (SQLException e){
            rollbackQuietly(conn);
            LOG.error("error executing: " + script, e);
            throw new DaoException("SQL Dao Failed. Check if the table exists / if the desired information has been parsed and stored in the database\n" + e.toString());
        } finally {
            closeQuietly(conn);
        }
    }

    public String translateSqlScript(String script) {
        if (dialect == SQLDialect.POSTGRES) {
            script = script.replaceAll(
                    "(?i) BIGINT AUTO_INCREMENT ", " BIGSERIAL "
            );
            if (script.toLowerCase().contains(" index ")) {
                script = script.replaceAll(
                        "(?i) IF NOT EXISTS ", " "
                );
            }
            return script.toUpperCase();
        }
        return script;
    }

    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOG.warn("Failed to close connection: ", e);
            }
        }
    }

    /**
     * Optimizes the performance of the database.
     * On postgres this translates to vacuum analyze.
     * On h2 it does nothing.
     */
    public void optimize() throws DaoException {
        if (dialect == SQLDialect.POSTGRES) {
            Connection conn=null;
            try {
                conn = getConnection();
                conn.setAutoCommit(true);
                Statement st = conn.createStatement();
                st.execute("VACUUM ANALYZE VERBOSE;");
                st.close();
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); } catch (Exception e) {}
                    closeQuietly(conn);
                }
            }
        }
    }

    /**
     * Optimizes the performance of the database for some table.
     * On postgres this translates to vacuum analyze.
     * On h2 it does nothing.
     */
    public void optimize(String table) throws DaoException {
        if (dialect == SQLDialect.POSTGRES) {
            Connection conn=null;
            try {
                conn = getConnection();
                conn.setAutoCommit(true);
                Statement st = conn.createStatement();
                st.execute("VACUUM ANALYZE " + table);
                st.close();
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); } catch (Exception e) {}
                    closeQuietly(conn);
                }
            }
        }
    }

    /**
     * Optimizes the performance of the database for some table.
     * On postgres this translates to vacuum analyze.
     * On h2 it does nothing.
     */
    public void optimize(Table table) throws DaoException {
        optimize(table.getName());
    }

    /**
     * In general, open connections are reclaimed and harmless
     */
    public void close() throws IOException {
        if (dialect == SQLDialect.H2) {
            Statement stm = null;
            Connection cnx = null;
            try {
                cnx = getConnection();
                stm = cnx.createStatement();
                stm.execute("SHUTDOWN;");
                stm.close();
            } catch (SQLException e) {
                throw new IOException(e);
            } finally {
                closeQuietly(cnx);
            }
        }
    }

    public static class WpDsProvider extends Provider<WpDataSource> {

        /**
         * Creates a new provider instance.
         * Concrete implementations must only use this two-argument constructor.
         *
         * @param configurator
         * @param config
         */
        public WpDsProvider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return WpDataSource.class;
        }

        @Override
        public String getPath() {
            return "dao.dataSource";
        }

        @Override
        public WpDataSource get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            try {
                Class.forName(config.getString("driver"));
                BoneCPDataSource ds = new BoneCPDataSource();
//                ds.setCloseConnectionWatch(true);
                ds.setDisableConnectionTracking(true);
                ds.setJdbcUrl(config.getString("url"));
                ds.setUsername(config.getString("username"));
                ds.setPassword(config.getString("password"));
                String partitions = config.getString("partitions");
                if (partitions.equals("default")) {
                    ds.setPartitionCount(Math.max(8, Runtime.getRuntime().availableProcessors()));
                } else {
                    ds.setPartitionCount(Integer.valueOf(partitions));
                }

                int cnxPerPartition = config.getInt("connectionsPerPartition");
                while (cnxPerPartition * ds.getPartitionCount() < getMinimumReasonableConnections()) {
                    cnxPerPartition++;
                }
                if (cnxPerPartition != config.getInt("connectionsPerPartition")) {
                    LOG.warn("Raised connections per partition to " + cnxPerPartition);
                }
                ds.setMaxConnectionsPerPartition(cnxPerPartition);

                return new WpDataSource(ds);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }

    }

    private static int getMinimumReasonableConnections() {
        return 2 * WpThreadUtils.getMaxThreads() + 12;
    }
}
