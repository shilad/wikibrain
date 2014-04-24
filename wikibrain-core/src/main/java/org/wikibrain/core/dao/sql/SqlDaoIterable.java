package org.wikibrain.core.dao.sql;

import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.wikibrain.core.dao.DaoException;

import java.sql.Connection;
import java.util.Iterator;

/**
 * @author Ari Weiland
 *
 * This iterable is used by the SQL Daos to convert a jOOQ Cursor into an
 * Iterable of the appropriate class. E is the output class type that is
 * iterated over, and T is an arbitrary object, such as an int that represents
 * a single element E, and is initially iterated over to generate E.
 * <p>
 * For complex entities where an output E is composed of multiple input Records,
 * T should be an element of a collection that has a one-to-one correspondence
 * to each E to be generated. For example, for UniversalPage, T might be an
 * Integer from a collection of UniversalPage IDs.
 * <p>
 * For simple entities where an output E is composed of a single input Record,
 * a {@link SimpleSqlDaoIterable} should be used. It wraps T as a Record and
 * provides a simpler constructor.
 * <p>
 * This iterable can only be iterated over once, and will throw exceptions
 * if a user tries otherwise.
 */
public abstract class SqlDaoIterable<E, T> implements Iterable<E> {
    protected Cursor<Record> result;
    protected Iterator<T> iterator;
    protected Connection conn;

    protected boolean usedUp = false;
    protected boolean finished = false;

    public SqlDaoIterable(Cursor<Record> result, Iterator<T> iterator, DSLContext context){
        this(result, iterator, JooqUtils.getConnection(context));

    }

    /**
     * Constructs a SqlDaoIterable that generates E objects from result.
     * The iterator must contain items that have a one-to-one correspondence
     * with the E objects contained in the Iterable that will be outputted.
     * @param result a collection of Records to be converted into outputs
     * @param iterator an iterator with a one-to-one relationship with the output iterable
     */
    public SqlDaoIterable(Cursor<Record> result, Iterator<T> iterator, Connection conn){
        this.result = result;
        this.iterator = iterator;
        this.conn = conn;
    }

    /**
     * Abstract method to be implemented at use. Describes how the SqlDaoIterable
     * converts T items from the input iterator to E items to be outputted.
     * @param item an element from the input iterator.
     * @return an object of class E
     * @throws DaoException
     */
    public abstract E transform(T item) throws DaoException;

    /**
     * Closes this iterable, disabling all functionality.
     */
    public void close() {
        usedUp = true;
        finished = true;
//        while (iterator.hasNext()) {
//            iterator.next();
//        }
        if (!result.isClosed()) {
            result.close();
        }
        AbstractSqlDao.quietlyCloseConn(conn);
    }

    @Override
    public Iterator<E> iterator() {
        if (usedUp) {
            throw new IllegalStateException("SqlDaoIterable can only be iterated over once.");
        }
        usedUp = true;
        return new Iterator<E>() {

            @Override
            public boolean hasNext() {
                try {
                    finished = !iterator.hasNext();
                    if (finished) {
                        close();
                    }
                    return !finished;
                } catch (Exception e) {
                    close();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public E next() {
                try {
                    T item = iterator.next();
                    if (finished || item == null) {
                        finished = true;
                        close();
                        return null;
                    }
                    return transform(item);
                } catch (Exception e) {
                    close();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
