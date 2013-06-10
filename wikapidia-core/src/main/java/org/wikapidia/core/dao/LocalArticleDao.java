package org.wikapidia.core.dao;

import org.wikapidia.core.model.LocalArticle;

import javax.sql.DataSource;

public abstract class LocalArticleDao extends LocalPageDao<LocalArticle> {

    /**
     *
     * @param ds
     * @throws DaoException
     */
    public LocalArticleDao(DataSource ds) throws DaoException {
        super(ds);
    }
}
