package org.wikibrain.core.dao.sql;

import org.apache.commons.io.FileUtils;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.model.MetaInfo;

import java.io.*;
import java.util.Date;

public class SqlCache {
    private final MetaInfoDao metaDao;
    private File directory;

    public SqlCache(MetaInfoDao metaDao, File directory) throws DaoException {
        this.metaDao = metaDao;
        this.directory=directory;
        if (!this.directory.isDirectory()) {
            throw new IllegalArgumentException("" + directory + " is not a valid directory");
        }
    }

    /**
     * Save a named object to the cache. Name is a unique identifier for the object
     * The object is saved in some/standard/directory/passed/to/AbstractSqlConstructor
     * @param name
     * @param object
     * @throws DaoException
     */
    public void put(String name, Object object) throws DaoException {
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

    /**
     * Removes a cached entity if it exists.
     * @param name
     */
    public void remove(String name) {
        FileUtils.deleteQuietly(getCacheFile(name));
    }

    private File getCacheFile(String name) {
        return new File(directory, name);
    }

    /**
     * Returns the object if it exists and is up to date, otherwise returns null.
     *
     * @param name Name of the object as passed to "put"
     * @param dependsOn List of classes the object depends on.
     *                  The cache is up to date iff for each class k in dependsOn:
     *                  - The MetaInfoDao knows about k
     *                  - The cache entry was created after k was last updated.
     * @return
     * @throws DaoException
     */
    public Object get(String name, Class ... dependsOn) throws DaoException {
        File cacheFile = getCacheFile(name);
        if (!cacheFile.isFile()) {
            return null;
        }
        Date cacheTstamp = new Date(cacheFile.lastModified());
        for (Class klass : dependsOn) {
            MetaInfo info = metaDao.getInfo(klass);
            if (info == null) {
                throw new DaoException("when looking for " + name + ", no info about class " + klass);
            }
            Date tableTstamp = info.getLastUpdated();
            if (tableTstamp == null || tableTstamp.after(cacheTstamp)) {
                return null;
            }
        }
        try {
            FileInputStream fis = new FileInputStream(getCacheFile(name));
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
}
