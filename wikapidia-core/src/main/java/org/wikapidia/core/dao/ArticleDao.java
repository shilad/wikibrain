package org.wikapidia.core.dao;


import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.Article;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 */
public class ArticleDao {

    private DataSource ds;

    public ArticleDao(DataSource dataSource) {
        ds = dataSource;
    }

    public Article get(int aId) throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn, SQLDialect.H2);
        Record record = context.select().
                                from(Tables.ARTICLE).
                                where(Tables.ARTICLE.ID.equal(aId)).
                                fetchOne();
        if (record == null) { return null; }
        Article a = buildArticle(record);
        conn.close();
        return a;
    }

    public void save(Article article) throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn,SQLDialect.H2);
        context.insertInto(Tables.ARTICLE).values(
            article.getId(),
            article.getTitle(),
            article.getNs().getValue(),
            article.getType().getValue(),
            article.getText()
        ).execute();
        conn.close();
    }

    public WikapidiaIterable<Article> query(String title) throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn,SQLDialect.H2);
        Cursor<Record> result = context.select().
                                        from(Tables.ARTICLE).
                                        where(Tables.ARTICLE.TITLE.likeIgnoreCase(title)).
                                        fetchLazy();
        conn.close();

        return buildArticles(result);
    }

    public WikapidiaIterable<Article> query(Article.NameSpace ns) throws SQLException{
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn,SQLDialect.H2);
        Cursor<Record> result = context.select().
                                        from(Tables.ARTICLE).
                                        where(Tables.ARTICLE.NS.equal(ns.getValue())).
                                        fetchLazy();
        conn.close();
        return buildArticles(result);
    }

    public WikapidiaIterable<Article> query(String title, Article.NameSpace ns) throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn,SQLDialect.H2);
        Cursor<Record> result = context.select().
                                        from(Tables.ARTICLE).
                                        where(Tables.ARTICLE.TITLE.likeIgnoreCase(title)).
                                        and(Tables.ARTICLE.NS.equal(ns.getValue())).
                                        fetchLazy();
        conn.close();
        return buildArticles(result);
    }

    public int getDatabaseSize() throws SQLException {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn,SQLDialect.H2);
        Result<Record> result = context.select().
                                        from(Tables.ARTICLE).
                                        fetch();
        conn.close();
        return result.size();
    }


    private Article buildArticle(Record record){
        return new Article(
            record.getValue(Tables.ARTICLE.ID),
            record.getValue(Tables.ARTICLE.TITLE),
            Article.NameSpace.intToNS(record.getValue(Tables.ARTICLE.NS)),
            Article.PageType.values()[record.getValue(Tables.ARTICLE.PTYPE)],
            record.getValue(Tables.ARTICLE.TEXT),
            null
        );
    }

    private WikapidiaIterable<Article> buildArticles(Cursor<Record> result){
        return new WikapidiaIterable<Article>(result,
                    new DaoTransformer<Article>() {
                        @Override
                        public Article transform(Record r) {
                            return buildArticle(r);
                        }
                    }
        );
    }

/*    private ArrayList<Article> buildArticles(Result<Record> result){
        ArrayList<Article> articles = new ArrayList<Article>();
        for (Record record: result){
            Article a = new Article(
                    record.getValue(Tables.ARTICLE.ID),
                    record.getValue(Tables.ARTICLE.TITLE),
                    Article.NameSpace.intToNS(record.getValue(Tables.ARTICLE.NS)),
                    Article.PageType.values()[record.getValue(Tables.ARTICLE.PTYPE)],
                    record.getValue(Tables.ARTICLE.TEXT)
            );
            articles.add(a);
        }
        return articles;
    }*/
}
