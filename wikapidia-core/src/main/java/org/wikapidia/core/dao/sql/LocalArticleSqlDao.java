package org.wikapidia.core.dao.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class LocalArticleSqlDao extends LocalArticleDao {

    public LocalArticleSqlDao(DataSource ds) throws DaoException {
        super(ds);
    }

    @Override
    public void beginLoad() throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void save(LocalArticle page) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.LOCAL_PAGE, Tables.LOCAL_PAGE.LANG_ID, Tables.LOCAL_PAGE.PAGE_ID, Tables.LOCAL_PAGE.TITLE, Tables.LOCAL_PAGE.PAGE_TYPE).values(
                    page.getLanguage().getId(),
                    page.getLocalId(),
                    page.getTitle().toString(),
                    (short) page.getPageType().ordinal()
            ).execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void endLoad() throws DaoException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LocalArticle getByPageId(Language language, int pageId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.equal(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                    fetchOne();
            return buildLocalArticle(record);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public LocalArticle getByTitle(Language language, Title title, PageType ns) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.eq(title.toString())).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                    and(Tables.LOCAL_PAGE.NS.eq(ns.getNamespace().getValue())).
                    fetchOne();
            if (record == null) { return null; }
            return buildLocalArticle(record);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
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
        return getByTitle(language, title, PageType.ARTICLE);
    }

    /**
     * Returns a Map of LocalArticles based on language and a collection of titles, with namespace assumed as ARTICLE.
     *
     * @param language the language of the articles
     * @param titles the titles to be searched for
     * @return a Map of LocalArticles mapped to their titles
     * @throws DaoException
     */
    public Map<Title, LocalArticle> getByTitles(Language language, Collection<Title> titles) throws DaoException{
        return getByTitles(language, titles, PageType.ARTICLE);
    }

    /**
     * Build a LocalArticle from a database record representation
     *
     * @param record a database record
     * @return a LocalArticle representation of the given database record
     * @throws DaoException if the record is not an Article
     */
    private LocalArticle buildLocalArticle(Record record) throws DaoException {
        if (record == null ) { return null; }

        short langId = record.getValue(Tables.LOCAL_PAGE.LANG_ID);
        Language language = Language.getById(langId);
        int pageId = record.getValue(Tables.LOCAL_PAGE.PAGE_ID);
        Title title = new Title(record.getValue(Tables.LOCAL_PAGE.TITLE), LanguageInfo.getById(langId));

        PageType pagetype = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        if (!pagetype.equals(PageType.ARTICLE)){
            throw new DaoException("Tried to get ARTICLE, but found "+pagetype.name());
        }
        else {
            return new LocalArticle(language, pageId, title);
        }
    }
}
