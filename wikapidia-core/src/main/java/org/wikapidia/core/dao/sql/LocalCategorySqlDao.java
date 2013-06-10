package org.wikapidia.core.dao.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class LocalCategorySqlDao extends LocalCategoryDao{

    public LocalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public LocalCategory getByPageId(Language language, int pageId) throws DaoException{
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.equal(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal(language.getId())).
                    fetchOne();
            return buildLocalCategory(record);
        } catch (SQLException e) { throw new DaoException(e);
        } finally { quietlyCloseConn(conn);
        }
    }


    @Override
    public LocalCategory getByTitle(Language language, Title title, PageType ns) throws DaoException{
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.equal(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal(title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NS.equal(PageType.CATEGORY.getNamespace().getValue())).
                    fetchOne();
            return buildLocalCategory(record);
        } catch (SQLException e) { throw new DaoException(e);
        } finally { quietlyCloseConn(conn);
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
    public LocalCategory getByTitle(Language language, Title title) throws DaoException {
        return getByTitle(language, title, PageType.CATEGORY);
    }

    /**
     * Returns a Map of LocalCategories based on language and a collection of titles, with namespace assumed as CATEGORY.
     *
     * @param language the language of the categories
     * @param titles the titles to be searched for
     * @return a Map of LocalCategories mapped to their titles
     * @throws DaoException
     */
    public Map<Title, LocalCategory> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        return getByTitles(language, titles, PageType.CATEGORY);
    }

    /**
     * Build a LocalCategory from a database record representation
     *
     * @param record a database record
     * @return a LocalCategory representation of the given database record
     * @throws DaoException if the record is not a Category
     */
    private LocalCategory buildLocalCategory(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        PageType ptype = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        if (!ptype.equals(PageType.CATEGORY)){
            throw new DaoException("Tried to get CATEGORY, but found "+ptype.name());
        }
        else {
            return new LocalCategory(
                    lang,
                    record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                    title
            );
        }
    }
}
