package org.wikapidia.core.dao;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.model.Concept;
import java.sql.Connection;
import java.sql.DriverManager;
import org.wikapidia.core.jooq.Tables;

public class ConceptDao {
    public Concept get(int cID){
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            Record record = context.select().from(Tables.CONCEPT).where(Tables.CONCEPT.ID.equals(cID)).fetchOne();
            Concept c = new Concept(
                    record.getValue(Tables.CONCEPT.ID),
                    null
            );
            conn.close();
            return c;
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void save(Concept c){
        try{
            Connection conn = connect();
            DSLContext context = DSL.using(conn, SQLDialect.H2);
            context.insertInto(Tables.CONCEPT, Tables.CONCEPT.ID).values(c.getId(), c.getArticleIds())
            conn.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private Connection connect()throws Exception{
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("tmp/maven-test");
        return conn;
    }
}
