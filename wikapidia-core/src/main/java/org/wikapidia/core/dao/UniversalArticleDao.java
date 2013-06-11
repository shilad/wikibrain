package org.wikapidia.core.dao;

import org.wikapidia.core.model.UniversalArticle;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalArticleDao extends UniversalPageDao<UniversalArticle> {

    /**
     * Returns a UniversalArticle instance corresponding to the input universal ID
     * @param univId the universal ID to be retrieved
     * @return a UniversalArticle
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract UniversalArticle getById(int univId) throws DaoException;

    /**
     * Returns a map of UniversalArticles based on a collection of of universal IDs
     * @param univIds a collection of universal IDs
     * @return a map of universal IDs to UniversalArticles
     * @throws DaoException if there was an error retrieving the pages
     */
    public abstract Map<Integer, UniversalArticle> getByIds(Collection<Integer> univIds) throws DaoException;

}
