package org.wikapidia.spatial.core.dao;


import org.wikapidia.core.dao.DaoException;

/**
 * Created by Brent Hecht on 12/29/13.
 */
public interface SpatialDao<T> {

    /**
     * Analogue to get() in Dao
     * @param daoFilter
     * @return
     * @throws DaoException
     */
    public Iterable<T> get(SpatialDaoFilter daoFilter) throws DaoException;

}
