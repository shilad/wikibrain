package org.wikapidia.core.dao.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.Article;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class LocalArticleSqlDao extends LocalArticleDao {

    public LocalArticleSqlDao(DataSource ds) {
        super(ds);
    }

    @Override
    public LocalArticle getByPageId(Language language, int pageId) throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn, SQLDialect.H2);
        Record record = context.select().
                from(Tables.LOCAL_PAGE).
                where(Tables.LOCAL_PAGE.PAGE_ID.equal(pageId)).
                and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                fetchOne();
        LocalArticle a = buildLocalArticle(record);
        conn.close();
        return a;
    }

    @Override
    public LocalArticle getByTitle(Language language, Title title, PageType ns) throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn, SQLDialect.H2);
        Record record = context.select().
                from(Tables.LOCAL_PAGE).
                where(Tables.LOCAL_PAGE.TITLE.eq(title.toString())).
                and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                and(Tables.LOCAL_PAGE.NS.eq(ns.getNamespace().getValue())).
                fetchOne();
        if (record == null) { return null; }
        LocalArticle a = buildLocalArticle(record);
        conn.close();
        return a;
    }

    public LocalArticle getByTitle(Language language, Title title) throws SQLException {
        return getByTitle(language, title, PageType.ARTICLE);
    }
}
