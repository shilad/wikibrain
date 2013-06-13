package org.wikapidia.core.dao.sql;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.jooq.Tables;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SqlCache extends AbstractSqlDao{
    String directory;

    public SqlCache(DataSource dataSource) throws DaoException {
        super(dataSource);
        this.directory="./";
    }

    public SqlCache(DataSource dataSource, String directory) throws DaoException {
        super(dataSource);
        this.directory=directory;
    }

    public void makeLastModifiedDb () throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/table-modified-schema.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }


    // Updates the timestamp in table "table_modified" to the current time
    void updateTableLastModified(String tableName) throws DaoException {
        Connection conn = null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.update(Tables.TABLE_MODIFIED)
                   .set(Tables.TABLE_MODIFIED.LAST_MODIFIED,
                           new Timestamp(System.currentTimeMillis()));
        }catch (SQLException e){
            throw new DaoException(e);
        }

    }

    // Updates the timestamp in table "table_modified" to the current time
    void updateTableLastUpdated(String tableName) throws DaoException {
        Connection conn = null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.update(Tables.TABLE_MODIFIED)
                    .set(Tables.TABLE_MODIFIED.LAST_UPDATED,
                            new Timestamp(System.currentTimeMillis()));
        }catch (SQLException e){
            throw new DaoException(e);
        }

    }

    // Save a named object to the cache. Name is a unique identifier for the object
    // The object is saved in some/standard/directory/passed/to/AbstractSqlConstructor
    void saveToCache(String name, Object object) throws DaoException {
        try {
            FileOutputStream fos = new FileOutputStream(directory+name);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.close();
        }
        catch (IOException e){
            throw new DaoException(e);
        }
    }

    //A convenience method to save and update
    void saveToCache(String name, Object object, String tableName) throws DaoException{
        saveToCache(name,object);
        updateTableLastModified(tableName);
    }

    // Returns the object if it exists and is up to date, otherwise returns null
    Object get(String objectName, String tableName) throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select()
                   .from(Tables.TABLE_MODIFIED)
                   .where(Tables.TABLE_MODIFIED.TITLE.equal(tableName))
                   .fetchOne();
            if (record==null || record.getValue(Tables.TABLE_MODIFIED.LAST_MODIFIED)
                    .after(record.getValue(Tables.TABLE_MODIFIED.LAST_UPDATED))){
                return null;
            }
            FileInputStream fis = new FileInputStream(directory+objectName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object object = ois.readObject();
            ois.close();
            return object;
        }catch (FileNotFoundException f){
            return null;
        } catch (SQLException e){
            throw new DaoException(e);
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }


}
