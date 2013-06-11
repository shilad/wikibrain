package org.wikapidia.core.dao.unused;

import org.jooq.Record;

public abstract interface DaoTransformer<E> {
    public E transform(Record r);
}
