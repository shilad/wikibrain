package org.wikapidia.core.dao;


import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.Map;

public interface LocalCategoryDao extends LocalPageDao {

    @Override
    public abstract LocalCategory getById(Language language, int pageId) throws DaoException;

    public abstract LocalCategory getByTitle(Language language, Title title) throws DaoException;

    public abstract Map<Title, LocalCategory> getByTitles(Language language, Collection<Title> titles) throws DaoException;

}
