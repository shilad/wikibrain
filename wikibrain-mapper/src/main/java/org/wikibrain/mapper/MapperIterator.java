package org.wikibrain.mapper;

import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * An iterator for use by ConceptMapper children.
 *
 * @author Ari Weiland
 *
 */
public abstract class MapperIterator<E> implements Iterator<E> {

    public static final Logger LOG = LoggerFactory.getLogger(MapperIterator.class);

    private final Iterator input;
    private int nullCounter = 0;

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
        boolean temp = input.hasNext();
        if (!temp) {
            LOG.info("Null records: " + nullCounter);
        }
        return temp;
    }

    @Override
    public E next() {
        Object temp = input.next();
        while (temp == null) {
            temp = input.next();
            nullCounter++;
        }
        return transform(temp);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
