package org.wikapidia.core.dao;

import org.wikapidia.core.model.UniversalCategory;

import java.util.Collection;
import java.util.Map;

/**
 */
public interface UniversalCategoryDao extends UniversalPageDao<UniversalCategory> {

    public abstract UniversalCategory getById(int univId) throws DaoException;

    public abstract Map<Integer, UniversalCategory> getByIds(Collection<Integer> univIds) throws DaoException;

}
