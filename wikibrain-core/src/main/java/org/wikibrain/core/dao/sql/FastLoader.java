package org.wikibrain.core.dao.sql;

import org.apache.commons.lang3.StringUtils;
import org.jooq.TableField;
import org.jooq.tools.jdbc.JDBCUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.utils.WpThreadUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bulk loads data in batch form to speed up insertions.
 *
 * @author Shilad Sen
 */
public class FastLoader {

    // In benchmarks, there were no benefits to more than four workers.
    private static final int NUM_INSERTERS = Math.min(WpThreadUtils.getMaxThreads(), 4);

    // Have this in the pill indicates a shutdown request has occurred
    private static final Object POSION_PILL = new Object();

    static final Logger LOG = LoggerFactory.getLogger(FastLoader.class);
    static final int BATCH_SIZE = 1000;

    private boolean isPostGisLoader = false;
    private final WpDataSource ds;
    private final String table;
    private final String[] fields;

    // State of loader.
    static enum LoaderState {
        RUNNING,            // In normal working mode
        FAILED,             // Loader failed, it cannot be used anymore
        SHUTTING_DOWN,      // Parent thread triggered a shutdown
        SHUTDOWN            // Already shutdown
    }
    private volatile LoaderState loaderState = null;

    // Queue holding inserts destined for workers
    private BlockingQueue<Object[]> toInsert =
            new ArrayBlockingQueue<Object[]>(BATCH_SIZE * NUM_INSERTERS * 2);

    // Threads for workers
    private Thread [] workers = new Thread[NUM_INSERTERS];

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

        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        workerMain();
                    } catch (DaoException e) {
                        LOG.error("inserter failed", e);
                        loaderState = LoaderState.FAILED;
                        toInsert.clear();  // allow any existing puts to go through
                    } catch (SQLException e) {
                        LOG.error("inserter failed", e);
                        loaderState = LoaderState.FAILED;
                        toInsert.clear();  // allow any existing puts to go through
                    } catch (InterruptedException e) {
                        LOG.error("inserter interrupted", e);
                        loaderState = LoaderState.FAILED;
                        toInsert.clear();  // allow any existing puts to go through
                    }
                }
            });
            workers[i].start();
        }
        loaderState = LoaderState.RUNNING;
    }


    /**
     * Saves a single row of values to the datastore.
     */
    public void insert(Object ... values) throws DaoException {
        if (workers == null || loaderState != LoaderState.RUNNING) {
            throw new IllegalStateException("inserter thread in state " + loaderState);
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
            toInsert.put(values);
        } catch (InterruptedException e) {
            throw new DaoException(e);
        }
    }

    /**
     * Main loop for each worker.
     */
    private void workerMain() throws DaoException, SQLException, InterruptedException {
        boolean finished = false;

        Connection cnx = ds.getConnection();

        // Register PostGIS geometry extensions if necesary.
        if (isPostGisLoader){
            try {

                ((org.postgresql.PGConnection) cnx).addDataType(
                        "geometry",
                        Class.forName("org.postgis.PGgeometry"));
            }catch(ClassNotFoundException e){
                throw new DaoException("Could not find PostGIS geometry type. " +
                        "Is the PostGIS library in the class path?: " + e.getMessage());
            }
        }

        // Prepare SQL for the prepared statement
        String [] names = new String[fields.length];
        String [] questions = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            names[i] = fields[i];
            questions[i] = "?";
        }
        String sql = "INSERT INTO " +
                table + "(" + StringUtils.join(names, ",") + ") " +
                "VALUES (" + StringUtils.join(questions, ",") + ");";
        PreparedStatement statement = cnx.prepareStatement(sql);;

        // Main loop runs until we get a shutdown request (indicated by "POISON_PILL" in the buffer).
        // Even if we encounter an error, we try to continue.
        try {

            // Repeat until we get a shutdown request
            while (!finished && loaderState != LoaderState.FAILED) {

                // accumulate batch
                int batchSize = 0;
                while (!finished && batchSize < BATCH_SIZE && loaderState != LoaderState.FAILED) {
                    Object row[] = toInsert.poll(100, TimeUnit.MILLISECONDS);
                    if (row == null) {
                        // do nothing
                    } else if (row[0] == POSION_PILL) {
                        // Pass the poison pill to the next worker and mark ourselves as finished
                        toInsert.put(new Object[]{POSION_PILL});
                        finished = true;
                    } else {
                        // Store row in batch
                        batchSize++;
                        for (int i = 0; i < row.length; i++) {
                            if(row[i] != null && row[i].getClass().equals(java.lang.Character.class))
                                 statement.setObject(i + 1, row[i].toString());
                            else
                                statement.setObject(i + 1, row[i]);
                        }
                        statement.addBatch();
                    }
                }

                // Insert batch and clear for next use
                try {
                    statement.executeBatch();
                    cnx.commit();
                } catch (SQLException e) {
                    cnx.rollback();
                    while (e != null) {
                        LOG.error("insert batch failed, attempting to continue:", e);
                        e = e.getNextException();
                    }
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

    /**
     * Trigger a shutdown request.
     */
    public void endLoad() throws DaoException {
        try {
            if (loaderState == LoaderState.RUNNING) {
                toInsert.put(new Object[]{POSION_PILL});
            }
            loaderState = LoaderState.SHUTTING_DOWN;
        } catch (InterruptedException e) {
            throw new DaoException(e);
        }
        for (Thread inserter : workers) {
            if (inserter != null) {
                try {
                    inserter.join(60000);
                } catch (InterruptedException e) {
                    throw new DaoException(e);
                }
            }
        }
        loaderState = LoaderState.SHUTDOWN;
    }

    public void close() throws  DaoException {
        endLoad();
    }


    /**
     * Utility function: Get names of fields for table.
     * @param fields
     * @return
     */
    private static String[] getFieldNames(TableField[] fields) {
        String names[] = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            names[i] = fields[i].getName();
        }
        return names;
    }

}
