package org.wikapidia.core.dao;

import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.UniversalPage;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalPageDao<T extends UniversalPage> extends Loader<T> {

    /**
     * Returns a UniversalPage instance of the specified page type corresponding to the input universal ID
     * @param univId the universal ID to be retrieved
     * @param pageType the page type to be retrieved
     * @return a UniversalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract T getById(int univId, PageType pageType) throws DaoException;

    /**
     * Returns a map of UniversalPages of the specified page type by a collection of universal IDs
     * @param univIds a collection of universal IDs
     * @param pageType the page type to be retrieved
     * @return a map of universal IDs to UniversalPages
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract Map<Integer, T> getByIds(Collection<Integer> univIds, PageType pageType) throws DaoException;
}
