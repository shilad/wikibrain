package org.wikibrain.core.dao;

import org.wikibrain.core.lang.LanguageSet;

/**
 *
 * Ubiquitous Dao interface implemented by all Daos. Describes methods
 * to initiate the load process, to save items, to conclude the load process,
 * and an allpurpose get() method that relies on a {@link DaoFilter} to
 * retrieve an Iterable of items.
 *
 * @author Ari Weiland
 *
 */
public interface Dao<T> {

    /**
     * Removes all entities from the data store.
     * For a sql table, this will be a "drop."
     *
     * @throws DaoException
     */
    public void clear() throws DaoException;

    /**
     * Runs front-end processes on the database
     * @throws DaoException if there was an error connecting to the database
     */
    public void beginLoad() throws DaoException;

    /**
     * Saves an item to the database
     * @param item the item to be saved
     * @throws DaoException if there was an error saving the item
     */
    public void save(T item) throws DaoException;

    /**
     * Runs back-end processes on the database
     * @throws DaoException if there was an error connecting to the database
     */
    public void endLoad() throws DaoException;

    /**
     * Returns an Iterable of T objects that fit the filters specified by the DaoFilter.
     *
     * @param daoFilter a set of filters to limit the search
     * @return an Iterable of objects that fit the specified filters
     * @throws DaoException if there was an error retrieving the objects
     */
    public Iterable<T> get(DaoFilter daoFilter) throws DaoException;

    /**
     * Returns the number of objects that fit the filters specified by the DaoFilter
     * @param daoFilter a set of filters to limit the search
     * @return the number of objects that fit the specified filters
     * @throws DaoException if there was an error retrieving the objects
     */
    public int getCount(DaoFilter daoFilter) throws DaoException;

    /**
     * @return The set of loaded languages.
     * @throws DaoException
     */
    public LanguageSet getLoadedLanguages() throws DaoException;

}
