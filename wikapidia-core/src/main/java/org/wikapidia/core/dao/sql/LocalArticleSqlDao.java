package org.wikapidia.core.dao.sql;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalArticleSqlDao extends LocalPageSqlDao implements LocalArticleDao {

    public LocalArticleSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public LocalArticle getById(Language language, int pageId) throws DaoException{
        LocalPage page = super.getById(language, pageId);
        if(page.getPageType() == PageType.ARTICLE) {
            return new LocalArticle(
                    page.getLanguage(),
                    page.getLocalId(),
                    page.getTitle()
            );
        }
        else {
            throw new DaoException("Tried to get ARTICLE, but found " + page.getPageType().name());
        }
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
        LocalPage page = super.getByTitle(language, title, PageType.ARTICLE);
        if(page.getPageType() == PageType.ARTICLE) {
            return new LocalArticle(
                    page.getLanguage(),
                    page.getLocalId(),
                    page.getTitle()
            );
        }
        else {
            throw new DaoException("Tried to get ARTICLE, but found " + page.getPageType().name());
        }
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
        Map<Title, LocalPage> map = super.getByTitles(language, titles, PageType.ARTICLE);
        Map<Title, LocalArticle> newMap = new HashMap<Title, LocalArticle>();
        for (Title title : map.keySet()) {
            LocalArticle temp = new LocalArticle(
                    map.get(title).getLanguage(),
                    map.get(title).getLocalId(),
                    map.get(title).getTitle()
            );
            newMap.put(title, temp);
        }
        return newMap;
    }
}
