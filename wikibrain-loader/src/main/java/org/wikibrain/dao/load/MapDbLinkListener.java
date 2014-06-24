package org.wikibrain.dao.load;

import org.mapdb.DB;
import org.wikibrain.core.model.LocalLink;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Shilad Sen
 */
public class MapDbLinkListener implements Iterator<Long> {
    private static final long MIN_SORT_MEMORY = 100 * 1024 * 1024;  // 100MB
    private static final int LONG_MEMORY_BYTES = 50;                     // total guess...
    private static final Long POISON_PILL = new Long(Long.MIN_VALUE);

    private ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<Long>(10000);
    private volatile Long next = null;

    public MapDbLinkListener(DB.BTreeSetMaker dbMaker) {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        int presortMem = (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_SORT_MEMORY, heapMaxSize / 20));

        dbMaker.pumpIgnoreDuplicates();
        dbMaker.pumpPresort(presortMem / LONG_MEMORY_BYTES);
    }

    public void notify(LocalLink link) {
        try {
            queue.put(link.longHashCode());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            queue.put(POISON_PILL);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean fillBuffer() {
        if (next != null) {
            return true;
        }
        if (queue == null) {
            return false;
        }
        while (next == null) {
            try {
                Long l = queue.take();
                if (l == POISON_PILL) {
                    queue = null;
                    return false;
                }
                next = l;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    @Override
    public synchronized boolean hasNext() {
        return fillBuffer();
    }

    @Override
    public Long next() {
        if (fillBuffer()) {
            return next;
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
