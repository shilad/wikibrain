package org.wikapidia.core.dao.sql;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalCategorySqlDao extends LocalPageSqlDao implements LocalCategoryDao {

    public LocalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public LocalCategory getById(Language language, int pageId) throws DaoException{
        LocalPage page = super.getById(language, pageId);
        if(page.getPageType() == PageType.CATEGORY) {
            return new LocalCategory(
                    page.getLanguage(),
                    page.getLocalId(),
                    page.getTitle()
            );
        }
        else {
            throw new DaoException("Tried to get CATEGORY, but found " + page.getPageType().name());
        }
    }

    /**
     * Returns a LocalCategory based on language and title, with namespace assumed as CATEGORY.
     *
     * @param language the language of the category
     * @param title the title of the category to be searched for
     * @return a LocalCategory object
     * @throws DaoException
     */
    @Override
    public LocalCategory getByTitle(Language language, Title title) throws DaoException {
        LocalPage page = super.getByTitle(language, title, PageType.CATEGORY);
        if(page.getPageType() == PageType.CATEGORY) {
            return new LocalCategory(
                    page.getLanguage(),
                    page.getLocalId(),
                    page.getTitle()
            );
        }
        else {
            throw new DaoException("Tried to get CATEGORY, but found " + page.getPageType().name());
        }
    }

    /**
     * Returns a Map of LocalCategories based on language and a collection of titles, with namespace assumed as CATEGORY.
     *
     * @param language the language of the categories
     * @param titles the titles to be searched for
     * @return a Map of LocalCategories mapped to their titles
     * @throws DaoException
     */
    @Override
    public Map<Title, LocalCategory> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        Map<Title, LocalPage> map = super.getByTitles(language, titles, PageType.CATEGORY);
        Map<Title, LocalCategory> newMap = new HashMap<Title, LocalCategory>();
        for (Title title : map.keySet()) {
            LocalCategory temp = new LocalCategory(
                    map.get(title).getLanguage(),
                    map.get(title).getLocalId(),
                    map.get(title).getTitle()
            );
            newMap.put(title, temp);
        }
        return newMap;
    }
}
