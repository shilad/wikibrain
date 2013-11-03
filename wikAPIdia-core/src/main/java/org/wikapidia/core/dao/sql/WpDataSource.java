package org.wikapidia.core.dao.sql;

import com.jolbox.bonecp.BoneCPDataSource;
import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
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
//            DSLContext ctx = DSL.using(c, dialect, settings);
//            settings = new Settings()
//                .withRenderMapping(new RenderMapping()
//                        .withSchemata(
//                                new MappedSchema().withInput("PUBLIC")
//                                        .withOutput("public")));

        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DSLContext getJooq() throws DaoException {
        try {
            return DSL.using(dataSource.getConnection(), dialect, settings);
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
            script = IOUtils.toString(AbstractSqlDao.class.getResource(name));
        } catch (IOException e) {
            throw new DaoException(e);
        }
        script = translateSqlScript(script);
        Connection conn=null;
        try {
            conn = dataSource.getConnection();
            conn.createStatement().execute(script);
        } catch (SQLException e){
            LOG.warning("error executing: " + script);
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
        public WpDataSource get(String name, Config config) throws ConfigurationException {
            try {
                Class.forName(config.getString("driver"));
                BoneCPDataSource ds = new BoneCPDataSource();
                ds.setJdbcUrl(config.getString("url"));
                ds.setUsername(config.getString("username"));
                ds.setPassword(config.getString("password"));
                ds.setPartitionCount(Runtime.getRuntime().availableProcessors());
                ds.setMaxConnectionsPerPartition(3);
                return new WpDataSource(ds);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }

    }
}
