package org.wikapidia.core.dao.sql;

import org.jooq.Record;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalCategorySqlDao extends LocalPageSqlDao<LocalCategory> implements LocalCategoryDao {

    public LocalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
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
        return super.getByTitle(language, title, PageType.CATEGORY);
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
        return super.getByTitles(language, titles, PageType.CATEGORY);
    }


    @Override
    protected LocalCategory buildLocalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        PageType pageType = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        if (pageType != PageType.CATEGORY) {
            throw new DaoException("Tried to get CATEGORY, but found " + pageType);
        }
        return new LocalCategory(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title
        );
    }
}
