package org.wikapidia.core.dao;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.Link;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 */
public class LinkDao {

    public Link get(int wpId) {
        Connection conn = null;
        try{
            conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().from(Tables.LINK).where(Tables.LINK.ARTICLE_ID.equal(wpId)).fetchOne();
            return new Link(
                record.getValue(Tables.LINK.TEXT),
                record.getValue(Tables.LINK.ARTICLE_ID),
                false
            );
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException ignore) {}
            }
        }
        return null;
    }

    public Link query(String wpText) {
        Connection conn = null;
        try{
            conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().from(Tables.LINK).where(Tables.LINK.TEXT.equal(wpText)).fetchOne();
            return new Link(
                    record.getValue(Tables.LINK.TEXT),
                    record.getValue(Tables.LINK.ARTICLE_ID),
                    false
            );
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException ignore) {}
            }
        }
        return null;
    }

    private Connection connect()throws Exception{
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("tmp/maven-test");
    }
}
