package org.wikibrain.loader;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.wikibrain.core.model.LocalLink;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Shilad Sen
 */
public class LocalLinkSet {
    private static final long MIN_SORT_MEMORY = 100 * 1024 * 1024;  // 100MB
    private static final int LONG_MEMORY_BYTES = 30;                     // bit of a guess
    private static final Long POISON_PILL = new Long(Long.MIN_VALUE);
    private final DB db;
    private final DB.BTreeSetMaker setMaker;

    private ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<Long>(10000);
    private volatile Long next = null;
    private Thread worker = null;
    private Set<Long> set = null;

    public LocalLinkSet() {
        this.db = DBMaker
                .newTempFileDB()
                .mmapFileEnable()
                .transactionDisable()
                .asyncWriteEnable()
                .asyncWriteFlushDelay(100)
                .make();
        this.setMaker = db.createTreeSet("linkHashes");

        long heapMaxSize = Runtime.getRuntime().maxMemory();
        int presortMem = (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_SORT_MEMORY, heapMaxSize / 20));

        setMaker.pumpIgnoreDuplicates();
        setMaker.pumpPresort(presortMem / LONG_MEMORY_BYTES);

        worker = new Thread() {
            @Override
            public void run() {
                setMaker.pumpSource(new LinkHashIterator());
                set = setMaker.makeLongSet();
            }
        };
        worker.start();
    }

    public void addLink(LocalLink link) {
        try {
            queue.put(link.longHashCode());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void finish() {
        try {
            queue.put(POISON_PILL);
            synchronized (worker) {
                worker.wait();
                worker = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(LocalLink link) {
        return set.contains(link.longHashCode());
    }

    class LinkHashIterator implements Iterator<Long> {
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
                Long result = next;
                next = null;
                return result;
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
