package org.wikapidia.core.dao.sql;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.jooq.Tables;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SqlCache {
    private final WpDataSource ds;
    private File directory;

    public SqlCache(WpDataSource dataSource, File directory) throws DaoException {
        this.ds = dataSource;
        this.directory=directory;
        if (!this.directory.isDirectory()) {
            throw new IllegalArgumentException("" + directory + " is not a valid directory");
        }
    }

    public void makeLastModifiedDb () throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getDataSource().getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/table-modified-create-tables.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            AbstractSqlDao.quietlyCloseConn(conn);
        }
    }

    /**
     * Updates the timestamp in table "table_modified" to the current time
     */
    public void updateTableLastModified(String tableName) throws DaoException {
        DSLContext context = ds.getJooq();
        try{
            Timestamp now = new Timestamp(System.currentTimeMillis()/1000*1000);  //rounds to seconds
            int n = context.update(Tables.TABLE_MODIFIED)
                    .set(Tables.TABLE_MODIFIED.LAST_MODIFIED, now)
                    .where(Tables.TABLE_MODIFIED.TABLE_NAME.eq(tableName))
                    .execute();

            if (n == 0) {
                context.insertInto(Tables.TABLE_MODIFIED, Tables.TABLE_MODIFIED.TABLE_NAME, Tables.TABLE_MODIFIED.LAST_MODIFIED)
                    .values(tableName, now)
                    .execute();
            }
            JooqUtils.commit(context);
        } catch (RuntimeException e) {
            JooqUtils.rollbackQuietly(context);
            throw e;
        } finally {
            ds.freeJooq(context);
        }

    }

    /**
     * Save a named object to the cache. Name is a unique identifier for the object
     * The object is saved in some/standard/directory/passed/to/AbstractSqlConstructor
     * @param name
     * @param object
     * @throws DaoException
     */
    public void saveToCache(String name, Object object) throws DaoException {
        try {
            FileOutputStream fos = new FileOutputStream(getCacheFile(name));
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.close();
        }
        catch (IOException e){
            throw new DaoException(e);
        }
    }

    private File getCacheFile(String name) {
        return new File(directory, name);
    }

    /**
     * Returns the object if it exists and is up to date, otherwise returns null
     * @param objectName
     * @param tableNames List of table names whose data the cache depends on
     * @return
     * @throws DaoException
     */
    public Object get(String objectName, String ... tableNames) throws DaoException {
        if (!getCacheFile(objectName).isFile()) {
            return null;
        }
        Timestamp cacheTstamp = new Timestamp(getCacheFile(objectName).lastModified());
        for (String name : tableNames) {
            Timestamp tableTstamp = getLastModified(name);
            if (tableTstamp == null || tableTstamp.after(cacheTstamp)) {
                return null;
            }
        }
        try {
            FileInputStream fis = new FileInputStream(getCacheFile(objectName));
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object object = ois.readObject();
            ois.close();
            return object;
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

     public Timestamp getLastModified(String tableName) throws DaoException {
         DSLContext context = ds.getJooq();
         try {
             Record record = context.select()
                     .from(Tables.TABLE_MODIFIED)
                     .where(Tables.TABLE_MODIFIED.TABLE_NAME.equal(tableName))
                     .fetchOne();
             if (record == null) {
                 return null;
             } else {
                return record.getValue(Tables.TABLE_MODIFIED.LAST_MODIFIED);
             }
         } finally {
             ds.freeJooq(context);
         }
     }
}
