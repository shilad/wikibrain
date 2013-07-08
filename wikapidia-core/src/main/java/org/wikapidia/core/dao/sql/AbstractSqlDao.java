package org.wikapidia.core.dao.sql;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.wikapidia.core.dao.Dao;
import org.wikapidia.core.dao.DaoException;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ari Weiland, Shilad Sen
 *
 * A SQL Dao superclass that contains a few important parameters and utility methods
 * ubiquitous to all SQL Daos.
 */
public abstract class AbstractSqlDao implements Dao {
    public static final Logger LOG = Logger.getLogger(AbstractSqlDao.class.getName());


    public static final int DEFAULT_FETCH_SIZE = 1000;

    protected final SQLDialect dialect;
    protected DataSource ds;
    protected SqlCache cache;
    private int fetchSize = DEFAULT_FETCH_SIZE;

    // Used for directly loading csv files for databases that support it.
    private File csvFile = null;
    private CsvListWriter writer = null;
    private TableField [] fields = null;

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

    /**
     * Executes a sql resource on the classpath
     * @param name Resource path - e.g. "/db/local-page.schema.sql"
     * @throws DaoException
     */
    public void executeSqlResource(String name) throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource(name)
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    /**
     * Must return the list of fields to be inserted into the "main" table.
     * @return
     */
    protected abstract TableField [] getFields();

    /**
     * @return sql scripts for creating and dropping tables and indexes.
     */
    protected abstract SqlScript [] getSqlScripts();


    @Override
    public void clear() throws DaoException {
        executeScriptsWithType(SqlScript.Type.DROP_INDEXES);
        executeScriptsWithType(SqlScript.Type.DROP_TABLES);
        executeScriptsWithType(SqlScript.Type.CREATE_TABLES);
    }

    @Override
    public void beginLoad() throws  DaoException {
        executeScriptsWithType(SqlScript.Type.DROP_INDEXES);
        executeScriptsWithType(SqlScript.Type.CREATE_TABLES);

        if (JooqUtils.supportsCsvLoading(dialect)) {
            try {
                csvFile = File.createTempFile("table", "csv");
                csvFile.deleteOnExit();
                writer = new CsvListWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        new FileOutputStream(csvFile), "UTF-8")),
                        CsvPreference.STANDARD_PREFERENCE);
                fields = getFields();
                String [] names = new String[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    names[i] = fields[i].getName();
                }
                writer.writeHeader(names);
            } catch (IOException e) {
                throw new DaoException(e);
            }
        }
    }

    /**
     * Inserts values into the database.
     * Call this instead of direct sql inserts because the underlying code may optimize the inserts
     * by creating batch inserts or a cvs that can be directly loaded by the underlying database.
     * @param values
     */
    protected void insert(Table table, Object [] values) throws DaoException{
        if (JooqUtils.supportsCsvLoading(dialect)) {
            if (values.length != fields.length) {
                throw new IllegalArgumentException();
            }
            try {
                synchronized (writer) {
                    writer.write(values);
                }
            } catch (IOException e) {
                throw new DaoException(e);
            }
        } else {
            Connection conn = null;
            try {
                conn = ds.getConnection();
                DSLContext context = DSL.using(conn, dialect);
                context.insertInto(table).values(values).execute();
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                quietlyCloseConn(conn);
            }
        }
    }


    @Override
    public void endLoad() throws  DaoException {
        if (JooqUtils.supportsCsvLoading(dialect)) {
            Connection conn = null;
            try {
                writer.close();
                conn = ds.getConnection();
                JooqUtils.loadCsv(conn, fields[0].getTable(), csvFile);
            } catch (SQLException e) {
                throw new DaoException(e);
            } catch (IOException e) {
                throw new DaoException(e);
            } finally {
                quietlyCloseConn(conn);
            }
        }
        executeScriptsWithType(SqlScript.Type.CREATE_INDEXES);
    }

    private void executeScriptsWithType(SqlScript.Type type) throws DaoException {
        for (SqlScript script : getSqlScripts()) {
            if (script.getType() == type) {
                executeSqlResource(script.getPath());
            }
        }
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
