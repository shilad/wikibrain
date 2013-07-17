package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;

public interface LocalArticleDao extends LocalPageDao<LocalArticle>  {

    public abstract LocalArticle getByTitle(Language language, Title title) throws DaoException;

    public abstract Map<Title, LocalArticle> getByTitles(Language language, Collection<Title> titles) throws DaoException;

}
