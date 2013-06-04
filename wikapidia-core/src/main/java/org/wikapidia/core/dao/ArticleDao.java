package org.wikapidia.core.dao;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.model.Article;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

import org.wikapidia.core.jooq.Tables;

/**
 */
public class ArticleDao {
    public Article get(int wpId) {
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().from(Tables.ARTICLE).where(Tables.ARTICLE.ID.equal(wpId)).fetchOne();
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
            Connection conn = connect();
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

    public ArrayList<Article> query(String title){
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().from(Tables.ARTICLE).where(Tables.ARTICLE.TITLE.likeIgnoreCase(title)).fetch();
            return buildArticles(result);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Article> query(Article.NameSpace ns){
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().from(Tables.ARTICLE).where(Tables.ARTICLE.NS.equal(ns.getValue())).fetch();
            return buildArticles(result);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Article> query(String title, Article.NameSpace ns){
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn,SQLDialect.H2);
            Result<Record> result = context.select().from(Tables.ARTICLE).where(Tables.ARTICLE.TITLE.likeIgnoreCase(title)).and(Tables.ARTICLE.NS.equal(ns.getValue())).fetch();
            return buildArticles(result);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private Connection connect()throws Exception{
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("tmp/maven-test");
        return conn;
    }

    private ArrayList<Article> buildArticles(Result<Record> result){
        ArrayList<Article> articles=null;
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
