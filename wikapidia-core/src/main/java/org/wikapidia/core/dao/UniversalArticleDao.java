package org.wikapidia.core.dao;

import org.wikapidia.core.model.UniversalArticle;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalArticleDao extends UniversalPageDao<UniversalArticle> {

    public abstract UniversalArticle getById(int univId) throws DaoException;

    public abstract Map<Integer, UniversalArticle> getByIds(Collection<Integer> univIds) throws DaoException;

}
