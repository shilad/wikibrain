package org.wikapidia.core.dao.sql;

import org.jodah.typetools.TypeResolver;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.TableField;
import org.wikapidia.core.dao.Dao;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.LanguageSet;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A SQL Dao superclass that contains a few important parameters and utility methods
 * ubiquitous to all SQL Daos.
 *
 * @author Ari Weiland
 * @author Shilad Sen
 *
 */
public abstract class AbstractSqlDao<T> implements Dao<T> {
    public static final Logger LOG = Logger.getLogger(AbstractSqlDao.class.getName());

    public static final int DEFAULT_FETCH_SIZE = 1000;

    protected final SQLDialect dialect;
    private final String sqlScriptPrefix;
    private final TableField[] fields;
    private final Class<T> klass;
    private final MetaInfoSqlDao metaDao;
    protected DataSource ds;
    protected WpDataSource wpDs;
    protected SqlCache cache;
    private int fetchSize = DEFAULT_FETCH_SIZE;

    // Used for directly loading csv files for databases that support it.
    FastLoader loader;

    /**
     * @param dataSource Data source for jdbc connections
     * @param fields Ordered list of fields for inserts into the database.
     * @param sqlScriptPrefix The prefix used to find sql scripts in the class path
     *                        (e.g. "/db/raw-page"). This class will append "-create-tables.sql",
     *                        "-create-indexes.sql", "-drop-tables.sql", and "-drop-indexes.sql"
     *                        to the prefix to find sql scripts, and they all must exist.
     *                        The create-tables script must ONLY create the table for the dao
     *                        because it is used by the fast loader.
     * @throws DaoException
     */
    public AbstractSqlDao(WpDataSource dataSource, TableField [] fields, String sqlScriptPrefix) throws DaoException {
        Class<?>[] typeArguments = TypeResolver.resolveRawArguments(AbstractSqlDao.class, getClass());
        this.klass = (Class<T>) typeArguments[0];

        wpDs = dataSource;
        ds = wpDs.getDataSource();
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

        // TODO: pass this through the constructor
        if (this instanceof MetaInfoDao) {
            this.metaDao = (MetaInfoSqlDao) this;
        } else {
            this.metaDao = new MetaInfoSqlDao(wpDs);
        }
        this.fields = fields;
        this.sqlScriptPrefix = sqlScriptPrefix;
    }

    /**
     * Executes a sql resource on the classpath
     * @param name Resource path - e.g. "/db/local-page.schema.sql"
     * @throws DaoException
     */
    public void executeSqlResource(String name) throws DaoException {
        wpDs.executeSqlResource(name);
    }

    protected DSLContext getJooq() throws DaoException {
        return wpDs.getJooq();
    }

    protected void freeJooq(DSLContext context) {
        wpDs.freeJooq(context);
    }

    @Override
    public LanguageSet getLoadedLanguages() throws DaoException {
        return metaDao.getLoadedLanguages(klass);
    }

    @Override
    public void clear() throws DaoException {
        executeSqlScriptWithSuffix("-drop-indexes.sql");
        executeSqlScriptWithSuffix("-drop-tables.sql");
    }

    @Override
    public void beginLoad() throws  DaoException {
        executeSqlScriptWithSuffix("-drop-indexes.sql");
        executeSqlScriptWithSuffix("-create-tables.sql");
        if (fields != null) {
            loader = new FastLoader(ds, fields);
        }
    }

    /**
     * Inserts values into the database.
     * Call this instead of direct sql inserts because the underlying code may optimize the inserts
     * by creating batch inserts or a cvs that can be directly loaded by the underlying database.
     * @param values
     */
    protected void insert(Object ... values) throws DaoException{
        loader.load(values);
    }


    @Override
    public void endLoad() throws  DaoException {
        if (loader != null) {
            loader.endLoad();
        }
        executeSqlScriptWithSuffix("-create-indexes.sql");
    }

    /**
     * Executes the appropriate sql script with a particular suffix (.e.g. "-drop-tables.sql").
     * @param suffix
     * @throws DaoException
     */
    protected void executeSqlScriptWithSuffix(String suffix) throws DaoException {
        executeSqlResource(sqlScriptPrefix + suffix);
    }

    public void useCache(File dir) throws DaoException{
        cache = new SqlCache(wpDs, dir);
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
