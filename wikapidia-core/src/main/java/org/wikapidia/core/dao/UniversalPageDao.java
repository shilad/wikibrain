package org.wikapidia.core.dao;

import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalPageDao<T extends UniversalPage> extends DaoParent<T> {

    /**
     * Returns a UniversalPage instance corresponding to the input universal ID
     * @param univId the universal ID you want to retrieve
     * @return
     * @throws DaoException
     */
    public abstract T getById(int univId) throws DaoException;

    /**
     * Returns a map of UniversalPages by a collection of universal IDs
     * @param univIds a collection of universal IDs
     * @return a map of universal IDs to pages
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract Map<Integer, T> getByIds(Collection<Integer> univIds) throws DaoException;
}
