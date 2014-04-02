package org.wikapidia.core.dao.sql;

import com.jolbox.bonecp.BoneCPDataSource;
import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.Provider;
import org.wikapidia.core.dao.DaoException;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class WpDataSource {
    private static final Logger LOG = Logger.getLogger(WpDataSource.class.getName());

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
            throw new DaoException(e);
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
            LOG.log(Level.SEVERE    , "rollback failed: ", e);
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
            throw new DaoException(e);
        }
    }

    public void freeJooq(DSLContext context) {
        Connection conn = JooqUtils.getConnection(context);
        if (conn != null) {
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
            System.out.println(name);
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
                LOG.fine("executing:\n" + s + "\n=========================================\n");
                Statement st = conn.createStatement();
                st.execute(s + ";");
                st.close();
            }
            conn.commit();
        } catch (SQLException e){
            rollbackQuietly(conn);
            LOG.log(Level.SEVERE, "error executing: " + script, e);
            throw new DaoException(e);
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
                LOG.log(Level.WARNING, "Failed to close connection: ", e);
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
                ds.setJdbcUrl(config.getString("url"));
                ds.setUsername(config.getString("username"));
                ds.setPassword(config.getString("password"));
                ds.setPartitionCount(Runtime.getRuntime().availableProcessors());
                ds.setMaxConnectionsPerPartition(50);
                return new WpDataSource(ds);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }

    }
}
