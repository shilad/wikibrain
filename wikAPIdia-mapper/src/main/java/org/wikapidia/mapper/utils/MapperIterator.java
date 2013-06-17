package org.wikapidia.mapper.utils;

import java.util.Iterator;

/**
 */
public abstract class MapperIterator<E> implements Iterator<E> {

    private final Iterator input;

    public MapperIterator(Iterable input) {
        this.input = input.iterator();
    }

    /**
     * Defines how the MapperIterator converts the input objects to objects of class E
     * @param obj an element of the input Iterable
     * @return a new object of class E
     */
    public abstract E transform(Object obj);

    @Override
    public boolean hasNext() {
        return input.hasNext();
    }

    @Override
    public E next() {
        return transform(input);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
