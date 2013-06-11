package org.wikapidia.core.dao.unused;

import org.jooq.Cursor;
import org.jooq.Record;
import java.lang.UnsupportedOperationException;
import java.util.Iterator;

public class WikapidiaIterable<E> implements Iterable<E> {
    Cursor<Record> result;
    DaoTransformer<E> func;

    public WikapidiaIterable(Cursor<Record> result, DaoTransformer<E> func){
        this.result=result;
        this.func=func;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<Record> recordIterator = result.iterator();

            @Override
            public boolean hasNext() {
                return recordIterator.hasNext();
            }

            @Override
            public E next() {
                return func.transform(recordIterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
