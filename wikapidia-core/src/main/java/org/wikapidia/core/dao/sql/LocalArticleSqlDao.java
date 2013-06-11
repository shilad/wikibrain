package org.wikapidia.core.dao.sql;

import org.jooq.Record;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalArticleSqlDao extends LocalPageSqlDao<LocalArticle> implements LocalArticleDao{

    public LocalArticleSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    /**
     * Returns a LocalArticle based on language and title, with namespace assumed as ARTICLE.
     *
     * @param language the language of the article
     * @param title the title of the article to be searched for
     * @return a LocalArticle object
     * @throws DaoException
     */
    public LocalArticle getByTitle(Language language, Title title) throws DaoException {
        return super.getByTitle(language, title, PageType.ARTICLE);
    }

    /**
     * Returns a Map of LocalCategories based on language and a collection of titles, with namespace assumed as ARTICLE.
     *
     * @param language the language of the categories
     * @param titles the titles to be searched for
     * @return a Map of LocalCategories mapped to their titles
     * @throws DaoException
     */
    public Map<Title, LocalArticle> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        return super.getByTitles(language, titles, PageType.ARTICLE);
    }

    @Override
    protected LocalArticle buildLocalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        PageType pageType = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        if (pageType != PageType.ARTICLE) {
            throw new DaoException("Tried to get ARTICLE, but found " + pageType);
        }
        return new LocalArticle(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title
        );
    }
}
