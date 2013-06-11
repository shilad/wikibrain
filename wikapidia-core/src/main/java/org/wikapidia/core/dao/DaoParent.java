package org.wikapidia.core.dao;

/**
 */
public interface DaoParent<T> {

    public abstract void beginLoad() throws DaoException;

    public abstract void save(T item) throws DaoException;

    public abstract void endLoad() throws DaoException;

}
