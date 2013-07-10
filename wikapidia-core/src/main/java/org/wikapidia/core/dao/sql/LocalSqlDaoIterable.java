package org.wikapidia.core.dao.sql;

import org.jooq.Cursor;
import org.jooq.Record;

import java.sql.Connection;

/**
 * @author Ari Weiland
 *
 * A SqlDaoIterable for local entites that simplifies implementation a little.
 */
public abstract class LocalSqlDaoIterable<E> extends SqlDaoIterable<E, Record> {

    public LocalSqlDaoIterable(Cursor<Record> result, Connection conn) {
        super(result, result.iterator(), conn);
    }
}
