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

    /**
     * Returns an Iterable of T objects that fit the filters specified by the DaoFilter.
     * Possible filters are:
     * - Language collection
     * - NameSpace collection
     * - Redirect flag
     * - Disambiguation flag
     * - LocationType collection
     * - Source ID collection
     * - Destination ID collection
     * - Parseable flag
     * - Algorithm ID collection
     * Not all filters are apply to all objects. Collections are specified as a collection
     * of acceptable entries, while flags are booleans set to true, false, or null. Flags
     * and collections set to null will be ignored when the search is executed.
     *
     * @param daoFilter a set of filters to limit the search
     * @return an Iterable of objects that fit the specified filters
     * @throws DaoException if there was an error retrieving the objects
     */
    public abstract Iterable<T> get(DaoFilter daoFilter) throws DaoException;

}
