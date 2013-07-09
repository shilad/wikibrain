package org.wikapidia.core.dao.sql;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.tools.csv.CSVReader;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.quote.AlwaysQuoteMode;
import org.supercsv.quote.QuoteMode;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.utils.WpIOUtils;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Bulk loads data in batch form to speed up insertions.
 *
 * Right now this saves the data in a CSV and then loads it from the CSV.
 * While this is faster for databases that support loading from files, it
 * requires custom coding for each database and also requires that the
 * program is running on the same server as the database.
 *
 * TODO: improve batch performance for non-csv-loading dbs by using prepared
 * statements and batch execution.
 *
 * @author Shilad Sen
 */
public class FastLoader {
    static final Logger LOG = Logger.getLogger(FastLoader.class.getName());
    static final int BATCH_SIZE = 10000;

    private static final boolean CONSIDER_CSV= false;

    // these files could get huge, so be careful not to put them in /tmp which might be small.
    static final File DEFAULT_TMP_DIR = new File(".tmp");

    private final SQLDialect dialect;

    // Information for CSV
    private TableField[] fields;
    private DataSource ds;
    private Table table;
    private File csvFile = null;
    private CsvListWriter writer = null;

    // Information for non-csv
    private PreparedStatement statement;
    private Connection batchCnx;

    private int batchSize;

    private boolean useCsv = false;


    public FastLoader(DataSource ds, TableField[] fields) throws DaoException {
        this(ds, fields, DEFAULT_TMP_DIR);
    }

    public FastLoader(DataSource ds, TableField[] fields, File tmpDir) throws DaoException {
        this.ds = ds;
        this.table = fields[0].getTable();
        this.fields = fields;
        Connection cnx = null;
        try {
            cnx = ds.getConnection();
            this.dialect = JooqUtils.dialect(cnx);
            if (CONSIDER_CSV && dialect == SQLDialect.H2) {
                useCsv = true;
            }
            if (useCsv) {
                initCsv(tmpDir);
            } else {
                initSql();
            }
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
    }

    private void initCsv(File tmpDir) throws IOException {
        if (tmpDir.exists() && !tmpDir.isDirectory()) {
            FileUtils.forceDelete(tmpDir);
        }
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        csvFile = File.createTempFile("table", ".csv", tmpDir);
        csvFile.deleteOnExit();
        LOG.info("creating tmp csv for " + table.getName() + " at " + csvFile.getAbsolutePath());
        CsvPreference pref = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE)
                .useQuoteMode(new AlwaysQuoteMode())
                .useEncoder(new DefaultCsvEncoder())    // make it threadsafe
                .build();
        writer = new CsvListWriter(WpIOUtils.openWriter(csvFile), pref);
        String [] names = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            names[i] = fields[i].getName();
        }
        writer.writeHeader(names);
    }

    private void initSql() throws SQLException {
        batchCnx = ds.getConnection();
        String [] names = new String[fields.length];
        String [] questions = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            names[i] = fields[i].getName();
            questions[i] = "?";
        }
        String sql = "INSERT INTO " +
                table.getName() + "(" + StringUtils.join(names, ",") + ") " +
                "VALUES (" + StringUtils.join(questions, ",") + ");";
        statement = batchCnx.prepareStatement(sql);
        batchSize = 0;
    }
    /**
     * Saves a value to the datastore.
     * @param values
     * @throws DaoException
     */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    public void load(Object [] values) throws DaoException {
        if (values.length != fields.length) {
            throw new IllegalArgumentException();
        }
        try {
            if (useCsv) {
                synchronized (writer) {
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] != null && values[i] instanceof Date) {
                            values[i] = DATE_FORMAT.format((Date)values[i]);
                        }
                    }
                    writer.write(values);
                }
            } else {
                synchronized (statement) {
                    for (int i = 0; i < values.length; i++) {
                        statement.setObject(i + 1, values[i]);
                    }
                    statement.addBatch();
                    statement.clearParameters();
                    if (batchSize++ >= BATCH_SIZE) {
                        statement.executeBatch();
                        batchSize = 0;
                    }
                }
            }
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }

    public void endLoad() throws DaoException {
        if (useCsv) {
            LOG.info("beginning load of " + csvFile);
            if (dialect == SQLDialect.H2) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new DaoException(e);
                }
                loadH2();
            } else {
                throw new IllegalStateException(dialect.toString());
            }
            LOG.info("finished load of " + csvFile);
        } else {
            if (batchSize > 0) {
                try {
                    statement.executeBatch();
                } catch (SQLException e) {
                    throw new DaoException(e);
                }
            }
            AbstractSqlDao.quietlyCloseConn(batchCnx);
        }
    }

    private void loadH2() throws DaoException {
        Connection cnx = null;
        try {
            cnx = ds.getConnection();
            Statement s = cnx.createStatement();
            String quotedPath = csvFile.getAbsolutePath().replace("'", "''");
            s.execute("INSERT INTO " + table.getName() +
                    " SELECT * " +
                    " FROM CSVREAD('" + quotedPath + "', null, 'charset=UTF-8')");
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            FileUtils.deleteQuietly(csvFile);
            AbstractSqlDao.quietlyCloseConn(cnx);
        }
    }
}
