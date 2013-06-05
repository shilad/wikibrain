package org.wikapidia.core.dao;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.Link;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 */
public class LinkDao {

    public Link get(int wpId) {
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().from(Tables.LINK).where(Tables.LINK.ARTICLE_ID.equal(wpId)).fetchOne();
            Link l = new Link(
                    record.getValue(Tables.LINK.TEXT),
                    record.getValue(Tables.LINK.ARTICLE_ID),
                    false
            );
            conn.close();
            return l;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Link query(String wpText) {
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().from(Tables.LINK).where(Tables.LINK.TEXT.equal(wpText)).fetchOne();
            Link l = new Link(
                    record.getValue(Tables.LINK.TEXT),
                    record.getValue(Tables.LINK.ARTICLE_ID),
                    false
            );
            conn.close();
            return l;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void save(Link link) {
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            context.insertInto(Tables.LINK, Tables.LINK.ARTICLE_ID, Tables.LINK.TEXT).values(
                    link.getId(),
                    link.getText()
            );
            conn.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private Connection connect()throws Exception{
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("tmp/maven-test");
    }
}
