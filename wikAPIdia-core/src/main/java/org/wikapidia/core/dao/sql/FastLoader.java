package org.wikapidia.core.dao.sql;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.TableField;
import org.wikapidia.core.dao.DaoException;

import javax.sql.DataSource;
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
    static final Logger LOG = Logger.getLogger(FastLoader.class.getName());
    static final int BATCH_SIZE = 2000;

    private final DataSource ds;
    private final Table table;
    private final TableField[] fields;

    private BlockingQueue<Object[]> rowBuffer =
            new ArrayBlockingQueue<Object[]>(BATCH_SIZE * 2);

    private Thread inserter = null;
    volatile private boolean finished = false;

    public FastLoader(DataSource ds, TableField[] fields) throws DaoException {
        this.ds = ds;
        this.table = fields[0].getTable();
        this.fields = fields;

        inserter = new Thread(new Runnable() {
            public void run() {
                try {
                    insertBatches();
                } catch (DaoException e) {
                    LOG.log(Level.SEVERE, "inserter failed", e);
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "inserter failed", e);
                } catch (InterruptedException e) {
                    LOG.log(Level.SEVERE, "inserter interrupted", e);
                }
                inserter = null;
            }
        });
        inserter.start();
    }
    /**
     * Saves a value to the datastore.
     * @param values
     * @throws DaoException
     */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    public void load(Object [] values) throws DaoException {
        // Hack convert dates to Timestamps
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Date && !(values[i] instanceof Timestamp)) {
                values[i] = new Timestamp(((Date)values[i]).getTime());
            }
        }
        if (inserter == null) {
            throw new IllegalStateException("inserter thread exited!");
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
        Connection cnx = ds.getConnection();
        try {
            String [] names = new String[fields.length];
            String [] questions = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                names[i] = fields[i].getName();
                questions[i] = "?";
            }
            String sql = "INSERT INTO " +
                    table.getName() + "(" + StringUtils.join(names, ",") + ") " +
                    "VALUES (" + StringUtils.join(questions, ",") + ");";
            PreparedStatement statement = cnx.prepareStatement(sql);


            while (!(rowBuffer.isEmpty() && finished)) {
                // accumulate batch
                int batchSize = 0;
                while (batchSize < BATCH_SIZE) {
                    Object row[] = rowBuffer.poll(100, TimeUnit.MILLISECONDS);
                    if (row == null && finished) {
                        break;
                    }
                    if (row != null) {
                        batchSize++;
                        for (int i = 0; i < row.length; i++) {
                            statement.setObject(i + 1, row[i]);
                        }
                        statement.addBatch();
                    }
                }
                try {
                    statement.executeBatch();
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "insert batch failed, attempting to continue:", e);
                }
                statement.clearBatch();
            }
            if (!cnx.getAutoCommit())
                cnx.commit();
        } finally {
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
    }

    public void endLoad() throws DaoException {
        finished = true;
        if (inserter != null) {
            try {
                inserter.join(60000);
            } catch (InterruptedException e) {
                throw new DaoException(e);
            }
        }
    }

    public void close() throws  DaoException {
        endLoad();
    }
}
