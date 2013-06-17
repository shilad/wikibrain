package org.wikapidia.core.dao;

import org.wikapidia.core.model.UniversalCategory;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalCategoryDao extends UniversalPageDao<UniversalCategory> {

    /**
     * Returns a UniversalCategory instance corresponding to the input universal ID
     * @param univId the universal ID to be retrieved
     * @return a UniversalCategory
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract UniversalCategory getById(int univId, int algorithmId) throws DaoException;

    /**
     * Returns a map of UniversalCategories based on a collection of of universal IDs
     * @param univIds a collection of universal IDs
     * @return a map of universal IDs to UniversalCategories
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract Map<Integer, UniversalCategory> getByIds(Collection<Integer> univIds, int algorithmId) throws DaoException;

}
