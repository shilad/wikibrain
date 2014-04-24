package org.wikibrain.core.dao;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalCategory;
import org.wikibrain.core.model.Title;

import java.util.Collection;
import java.util.Map;

public interface LocalCategoryDao extends LocalPageDao<LocalCategory> {

    public LocalCategory getByTitle(Language language, Title title) throws DaoException;

    public Map<Title, LocalCategory> getByTitles(Language language, Collection<Title> titles) throws DaoException;

}
