package org.wikibrain.core.dao.sql;

import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.sql.Connection;

/**
 * @author Ari Weiland
 *
 * A SqlDaoIterable for simple entites that simplifies implementation a little.
 * Can only be used when there is a one-to-one relationship between output
 * elements E and input records.
 */
public abstract class SimpleSqlDaoIterable<E> extends SqlDaoIterable<E, Record> {

    public SimpleSqlDaoIterable(Cursor<Record> result, DSLContext context) {
        super(result, result.iterator(), context);
    }

    public SimpleSqlDaoIterable(Cursor<Record> result, Connection conn) {
        super(result, result.iterator(), conn);
    }
}
