package org.wikapidia.core.dao;

import org.jooq.Record;

public abstract interface DaoTransformer<E> {
    public E transform(Record r);
}
