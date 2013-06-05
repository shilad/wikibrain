package org.wikapidia.core.dao;


import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.Concept;

import javax.sql.DataSource;
import java.sql.Connection;

public class ConceptDao {

    private DataSource ds;

    public ConceptDao(DataSource dataSource) {
        ds = dataSource;
    }

    public Concept get(long cId){
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().
                                    from(Tables.CONCEPT).
                                    where(Tables.CONCEPT.ID.equal(cId)).
                                    fetchOne();
            if (record == null) {
                return null;
            }
            Concept c = new Concept(
                    record.getValue(Tables.CONCEPT.ID),
                    null
            );
            conn.close();
            return c;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void save(Concept c){
        try{
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            context.insertInto(Tables.CONCEPT, Tables.CONCEPT.ID).values(c.getId());
            conn.close();
        }
        catch(Exception e){
            e.printStackTrace();
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
}
