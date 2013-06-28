package org.wikapidia.core.dao;

import org.jooq.Cursor;
import org.jooq.Record;
import java.lang.UnsupportedOperationException;
import java.util.Iterator;

public abstract class SqlDaoIterable<E> implements Iterable<E> {
    Cursor<Record> result;

    private boolean usedUp = false;

    public SqlDaoIterable(Cursor<Record> result){
        this.result=result;
    }

    public abstract E transform(Record record);

    @Override
    public Iterator<E> iterator() {
        if (usedUp) {
            throw new IllegalStateException(
                    "SqlDaoIterable can only be iterated over once. " +
                    "We should change this to an iterator but for-each loops are nice."
            );
        }
        usedUp = true;
        return new Iterator<E>() {
            Iterator<Record> recordIterator = result.iterator();
            boolean finished = false;

            @Override
            public boolean hasNext() {
                if (!finished) {
                    finished = !recordIterator.hasNext();
                    if (finished) { result.close(); }
                }
                return !finished;
            }

            @Override
            public E next() {
                if (finished) {
                    return null;
                }
                Record r = recordIterator.next();
                if (r == null) {
                    finished = true;
                    result.close();
                    return null;
                }
                return transform(r);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
