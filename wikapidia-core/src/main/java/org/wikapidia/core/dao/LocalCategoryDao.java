package org.wikapidia.core.dao;

import org.wikapidia.core.model.LocalCategory;

import javax.sql.DataSource;

public abstract class LocalCategoryDao extends LocalPageDao<LocalCategory> {

    /**
     *
     * @param dataSource
     * @throws DaoException
     */
    public LocalCategoryDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }
}
