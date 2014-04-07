package org.wikapidia.core.dao.sql;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.tools.jdbc.JDBCUtils;
import org.wikapidia.core.dao.DaoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bulk loads data in batch form to speed up insertions.
 *
 * @author Shilad Sen
 */
public class FastLoader {

    private static final Object POSION_PILL = new Object();
    private boolean isPostGisLoader = false;

    static final Logger LOG = Logger.getLogger(FastLoader.class.getName());
    static final int BATCH_SIZE = 5;

    private final WpDataSource ds;
    private final String table;
    private final String[] fields;

    private BlockingQueue<Object[]> rowBuffer =
            new ArrayBlockingQueue<Object[]>(BATCH_SIZE * 2);

    static enum InserterState {
        RUNNING,            // In normal working mode
        FAILED,             // Loader failed, it cannot be used anymore
        SHUTTING_DOWN,      // Parent thread triggered a shutdown
        SHUTDOWN            // Already shutdown
    }

    private Thread inserter = null;
    private volatile InserterState inserterState = null;

    public FastLoader(WpDataSource ds, TableField[] fields) throws DaoException {
        this(ds, fields[0].getTable().getName(), getFieldNames(fields));
    }

    public FastLoader(WpDataSource ds, String table, String[] fields, boolean isPostGisLoader) throws DaoException {
        this(ds,table, fields);
        this.isPostGisLoader = isPostGisLoader;
    }

    public FastLoader(WpDataSource ds, String table, String[] fields) throws DaoException {
        this.ds = ds;
        this.table = table;
        this.fields = fields;

        inserter = new Thread(new Runnable() {
            public void run() {
                try {
                    insertBatches();
                } catch (DaoException e) {
                    LOG.log(Level.SEVERE, "inserter failed", e);
                    inserterState = InserterState.FAILED;
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "inserter failed", e);
                    inserterState = InserterState.FAILED;
                } catch (InterruptedException e) {
                    LOG.log(Level.SEVERE, "inserter interrupted", e);
                    inserterState = InserterState.FAILED;
                }
                rowBuffer.clear();  // allow any existing puts to go through
            }
        });
        inserter.start();
        inserterState = InserterState.RUNNING;
    }

    private static String[] getFieldNames(TableField[] fields) {
        String names[] = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            names[i] = fields[i].getName();
        }
        return names;
    }

    /**
     * Saves a value to the datastore.
     * @param values
     * @throws DaoException
     */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    public void load(Object ... values) throws DaoException {
        if (inserter == null || inserterState != InserterState.RUNNING) {
            throw new IllegalStateException("inserter thread in state " + inserterState);
        }
        // Hack convert dates to Timestamps
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Date && !(values[i] instanceof Timestamp)) {
                values[i] = new Timestamp(((Date)values[i]).getTime());
            }
        }
        if (values.length != fields.length) {
            throw new IllegalArgumentException();
        }
        try {
            rowBuffer.put(values);
        } catch (InterruptedException e) {
            throw new DaoException(e);
        }
    }

    private void insertBatches() throws DaoException, SQLException, InterruptedException {
        boolean finished = false;

        Connection cnx = ds.getConnection();
        if (isPostGisLoader){
            try {

                ((org.postgresql.PGConnection) cnx).addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
//                ((org.postgresql.PGConnection) cnx).addDataType("geometry", Class.forName("org.postgis.Multipolygon"));

            }catch(ClassNotFoundException e){
                throw new DaoException("Could not find PostGIS geometry type. Is the PostGIS library in the class path?: " + e.getMessage());
            }
        }

        PreparedStatement statement = null;
        try {
            String [] names = new String[fields.length];
            String [] questions = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                names[i] = fields[i];
                questions[i] = "?";
            }
            String sql = "INSERT INTO " +
                    table + "(" + StringUtils.join(names, ",") + ") " +
                    "VALUES (" + StringUtils.join(questions, ",") + ");";
            statement = cnx.prepareStatement(sql);


            while (!finished) {
                // accumulate batch
                int batchSize = 0;
                while (!finished && batchSize < BATCH_SIZE) {
                    Object row[] = rowBuffer.poll(100, TimeUnit.MILLISECONDS);
                    if (row == null) {
                        // do nothing
                    } else if (row[0] == POSION_PILL) {
                        finished = true;
                    } else {
                        batchSize++;
                        for (int i = 0; i < row.length; i++) {
                            statement.setObject(i + 1, row[i]);
                        }
                        statement.addBatch();
                    }
                }
                try {
                    statement.executeBatch();
                    cnx.commit();
                } catch (SQLException e) {
                    cnx.rollback();
                    LOG.log(Level.SEVERE, "insert batch failed, attempting to continue:", e);
                }
                statement.clearBatch();
            }
        } finally {
            if (statement != null) {
                JDBCUtils.safeClose(statement);
            }
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
    }

    public void endLoad() throws DaoException {
        try {
            if (inserterState == InserterState.RUNNING) {
                rowBuffer.put(new Object[]{POSION_PILL});
            }
            inserterState = InserterState.SHUTTING_DOWN;
        } catch (InterruptedException e) {
            throw new DaoException(e);
        }
        if (inserter != null) {
            try {
                inserter.join(60000);
            } catch (InterruptedException e) {
                throw new DaoException(e);
            }
        }
        inserterState = InserterState.SHUTDOWN;
    }

    public void close() throws  DaoException {
        endLoad();
    }
}
