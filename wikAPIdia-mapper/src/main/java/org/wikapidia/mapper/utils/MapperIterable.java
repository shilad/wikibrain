package org.wikapidia.mapper.utils;

import java.util.Collection;
import java.util.Iterator;

/**
 */
public abstract class MapperIterable<E> implements Iterable<E> {

    private final Iterable input;

    public MapperIterable(Iterable input) {
        this.input = input;
    }

    public abstract E transform(Iterator iterator);

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator iterator = input.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return transform(iterator);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
