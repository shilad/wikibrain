package org.wikapidia.core.dao;


import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.Article;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class ArticleDao {

    private DataSource ds;

    public ArticleDao(DataSource dataSource) {
        ds = dataSource;
    }

    public Article get(int wpId) {
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().
                                    from(Tables.ARTICLE).
                                    where(Tables.ARTICLE.ID.equal(wpId)).
                                    fetchOne();
            if (record == null) {
                return new Article(-1, "Oogalie Boogalie", Article.NameSpace.MAIN, Article.PageType.STANDARD);
            }
            Article a = new Article(
                record.getValue(Tables.ARTICLE.ID),
                record.getValue(Tables.ARTICLE.TITLE),
                Article.NameSpace.intToNS(record.getValue(Tables.ARTICLE.NS)),
                Article.PageType.values()[record.getValue(Tables.ARTICLE.PTYPE)]
            );
            conn.close();
            return a;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void save(Article article){
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            context.insertInto(Tables.ARTICLE).values(
                    article.getId(),
                    article.getTitle(),
                    article.getNs().getValue(),
                    article.getType().getValue()
            );
            conn.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<Article> query(String title){
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().
                                            from(Tables.ARTICLE).
                                            where(Tables.ARTICLE.TITLE.likeIgnoreCase(title)).
                                            fetch();
            conn.close();
            return buildArticles(result);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public List<Article> query(Article.NameSpace ns){
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().
                                            from(Tables.ARTICLE).
                                            where(Tables.ARTICLE.NS.equal(ns.getValue())).
                                            fetch();
            conn.close();
            return buildArticles(result);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public List<Article> query(String title, Article.NameSpace ns){
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().
                                            from(Tables.ARTICLE).
                                            where(Tables.ARTICLE.TITLE.likeIgnoreCase(title)).
                                            and(Tables.ARTICLE.NS.equal(ns.getValue())).
                                            fetch();
            conn.close();
            return buildArticles(result);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public int getDatabaseSize() {
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().
                    from(Tables.ARTICLE).
                    fetch();
            conn.close();
            return result.size();
        }
        catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    private ArrayList<Article> buildArticles(Result<Record> result){
        if (result == null) {
            return null;
        }
        ArrayList<Article> articles = null;
        for (Record record: result){
            Article a = new Article(
                    record.getValue(Tables.ARTICLE.ID),
                    record.getValue(Tables.ARTICLE.TITLE),
                    Article.NameSpace.intToNS(record.getValue(Tables.ARTICLE.NS)),
                    Article.PageType.values()[record.getValue(Tables.ARTICLE.PTYPE)]
            );
            articles.add(a);
        }
        return articles;
    }
}
