package org.wikapidia.core.dao;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.Link;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class LinkDao {

    private DataSource ds;

    public LinkDao(DataSource ds) {
        this.ds = ds;
    }

    public Link get(int lId) throws Exception {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn, SQLDialect.H2);
        Record record = context.select().
                                from(Tables.LINK).
                                where(Tables.LINK.ARTICLE_ID.equal(lId)).
                                fetchOne();
        if (record == null) {
            return null;
        }
        Link l = new Link(
                record.getValue(Tables.LINK.TEXT),
                record.getValue(Tables.LINK.ARTICLE_ID),
                false
        );
        conn.close();
        return l;
    }

    public List<Link> query(String lText) throws Exception {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn, SQLDialect.H2);
        Result<Record> result = context.select().
                                        from(Tables.LINK).
                                        where(Tables.LINK.TEXT.likeIgnoreCase(lText)).
                                        fetch();
        conn.close();
        return buildLinks(result);
    }

    public void save(Link link) throws Exception {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn, SQLDialect.H2);
        context.insertInto(Tables.LINK, Tables.LINK.ARTICLE_ID, Tables.LINK.TEXT).values(
                link.getId(),
                link.getText()
        ).execute();
        conn.close();
    }

    public int getDatabaseSize() throws Exception {
        Connection conn = ds.getConnection();
        DSLContext context = DSL.using(conn,SQLDialect.H2);
        Result<Record> result = context.select().
                from(Tables.ARTICLE).
                fetch();
        conn.close();
        return result.size();
    }

    private ArrayList<Link> buildLinks(Result<Record> result){
        ArrayList<Link> links = new ArrayList<Link>();
        for (Record record: result){
            Link a = new Link(
                    record.getValue(Tables.LINK.TEXT),
                    record.getValue(Tables.LINK.ARTICLE_ID),
                    false
            );
            links.add(a);
        }
        return links;
    }
}
