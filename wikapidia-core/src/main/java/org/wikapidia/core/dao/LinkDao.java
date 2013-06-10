//package org.wikapidia.core.dao;
//
//import org.jooq.*;
//import org.jooq.impl.DSL;
//import org.wikapidia.core.jooq.Tables;
//import org.wikapidia.core.model.Link;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// */
//public class LinkDao {
//
//    private DataSource ds;
//
//    public LinkDao(DataSource ds) {
//        this.ds = ds;
//    }
//
//    public Link get(int lId) throws SQLException {
//        Connection conn = ds.getConnection();
//        DSLContext context = DSL.using(conn, SQLDialect.H2);
//        Record record = context.select().
//                                from(Tables.LINK).
//                                where(Tables.LINK.ARTICLE_ID.equal(lId)).
//                                fetchOne();
//        if (record == null) { return null; }
//        Link l = buildLink(record);
//        conn.close();
//        return l;
//    }
//
//    public WikapidiaIterable<Link> query(String lText) throws SQLException {
//        Connection conn = ds.getConnection();
//        DSLContext context = DSL.using(conn, SQLDialect.H2);
//        Cursor<Record> result = context.select().
//                                        from(Tables.LINK).
//                                        where(Tables.LINK.TEXT.likeIgnoreCase(lText)).
//                                        fetchLazy();
//        conn.close();
//        return buildLinks(result);
//    }
//
//    public void save(Link link) throws SQLException {
//        Connection conn = ds.getConnection();
//        DSLContext context = DSL.using(conn, SQLDialect.H2);
//        context.insertInto(Tables.LINK, Tables.LINK.ARTICLE_ID, Tables.LINK.TEXT).values(
//                link.getId(),
//                link.getText()
//        ).execute();
//        conn.close();
//    }
//
//    public int getDatabaseSize() throws SQLException {
//        Connection conn = ds.getConnection();
//        DSLContext context = DSL.using(conn,SQLDialect.H2);
//        Result<Record> result = context.select().
//                from(Tables.ARTICLE).
//                fetch();
//        conn.close();
//        return result.size();
//    }
//
//    private Link buildLink(Record record){
//        return new Link(
//                record.getValue(Tables.LINK.TEXT),
//                record.getValue(Tables.LINK.ARTICLE_ID),
//                false
//        );
//    }
//
//    private WikapidiaIterable<Link> buildLinks(Cursor<Record> result){
//        return new WikapidiaIterable<Link>(result,
//                new DaoTransformer<Link>() {
//                    @Override
//                    public Link transform(Record r) {
//                        return buildLink(r);
//                    }
//                }
//        );
//    }
//
//    /*
//    private ArrayList<Link> buildLinks(Result<Record> result){
//        ArrayList<Link> links = new ArrayList<Link>();
//        for (Record record: result){
//            Link a = new Link(
//                    record.getValue(Tables.LINK.TEXT),
//                    record.getValue(Tables.LINK.ARTICLE_ID),
//                    false
//            );
//            links.add(a);
//        }
//        return links;
//    } */
//}
