package org.wikapidia.mapper;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ari Weiland
 *
 * An iterator for use by ConceptMapper children.
 *
 */
public abstract class MapperIterator<E> implements Iterator<E> {

    public static final Logger LOG = Logger.getLogger(MapperIterator.class.getName());

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
            LOG.log(Level.INFO, "Null records: " + nullCounter);
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
