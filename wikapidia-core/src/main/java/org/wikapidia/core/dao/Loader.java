package org.wikapidia.core.dao;

/**
 */
public interface Loader<T> {

    /**
     * Runs front-end processes on the database
     * @throws DaoException if there was an error connecting to the database
     */
    public abstract void beginLoad() throws DaoException;

    /**
     * Saves an item to the database
     * @param item the item to be saved
     * @throws DaoException if there was an error saving the item
     */
    public abstract void save(T item) throws DaoException;

    /**
     * Runs back-end processes on the database
     * @throws DaoException if there was an error connecting to the database
     */
    public abstract void endLoad() throws DaoException;

}
