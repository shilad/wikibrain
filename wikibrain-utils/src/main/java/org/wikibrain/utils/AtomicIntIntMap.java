package org.wikibrain.utils;

import gnu.trove.impl.PrimeFinder;
import gnu.trove.list.array.TIntArrayList;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of an int int map that uses open addressing.
 * Inspired by both Trove's primitive map and Cliff Click's non-locking hashtable
 * http://www.stanford.edu/class/ee380/Abstracts/070221_LockFreeHash.pdf
 *
 * The implementation is threadsafe and almost entirely lock-free.
 * Locks do occur while the the underlying array is expanded.
 *
 * If you're careful and spread an accurate guess of capacity,
 * peformance will be dramatically improved.
 *
 * State diagram follows
 *
 * if expandedTable != null, check it first
 *
 * Keys and values of zero with flag != FLAG_UNSET indicate fast operations are
 * in progress. System should try again ASAP.
 *
 * A value of 0 is represented by bitwise or'ing flags with FLAG_VALUE_ZERO
 * If this is set, value should be set to some non-zero value (a value of zero
 * indicates that an operation is in progress.
 *
 *
 * Free: (0, 0, FLAG_UNSET)
 *
 * Set: (!0, !0, FLAG_SET)
 *
 * Delete: (!0, 0, FLAG_DELETED)
 *
 * Expanding table:
 *
 *      The actual space for the extra table is allocated.
 *      The size of the table is expanded by 1 / load-factor, unless it's overly full, then 2 / load-factor.
 *      A background thread begins to copy values from the old to new table, as described below.
 *      Any puts also do a limited amount of help in table expansion.
 *      If the new table gets too full from new puts, all put threads block and help expansion.
 *
 *      The actual procedure for moving a single entry from the old to new table follows:
 *
 *      Step 1. Create copy of value in new table; new: (!0, !0, FLAG_OLD)
 *      Step 2. Verify original hasn't changed values or state, mark it as moved: old: (!0, *, FLAG_MOVED)
 *      Step 3. Release the new one as available: new: (!0, !0, FLAG_SET)
 *
 *
 * @author Shilad Sen
 */
public class AtomicIntIntMap {
    private static final Logger LOG = Logger.getLogger(AtomicIntIntMap.class.getName());

    public static final int DEFAULT_MISSING_KEY_VALUE = -1;

    // Slot is empty
    private static final byte FLAG_UNSET = 0;

    // Slot is set and up to date
    private static final byte FLAG_SET = 1;

    // Slot is set, but may be out of date
    private static final byte FLAG_OLD = 2;

    // Slot has been deleted
    private static final byte FLAG_DELETED = 3;

    // Slot has been moved to expanded table
    private static final byte FLAG_MOVED = 4;

    // Slot contains a value of zero (bitwise or'ed)
    private static final byte FLAG_VALUE_ZERO = 64;

    private static final byte FLAG_SET_ZERO = FLAG_SET | FLAG_VALUE_ZERO;

    private volatile Table table;
    private volatile Table expandedTable;

    // Number of elements in the map
    private final AtomicInteger numElements = new AtomicInteger();

    // Possible number of elements after all in-progress puts succeed
    private final AtomicInteger potentialNumElements = new AtomicInteger();

    // Value to return for missing keys
    private int missingKeyValue = DEFAULT_MISSING_KEY_VALUE;

    // Value associated with the key zero.
    private transient Integer zeroValue = null;

    private double loadFactor = 0.5;

    private volatile ResizeCounter resizeCounter;


    public AtomicIntIntMap() {
        this(5);
    }

    /**
     * Create a new keys with the specified capacity.
     * Ideally, the capacity will be at least the number of total elements * (1 / loadFactor)
     * to prevent future expansions.
     *
     * @param capacity
     */
    public AtomicIntIntMap(int capacity) {
        table = new Table(capacity);
    }

    /**
     * Returns true iff the keys contains the specified key.
     * If the key is added before contains is called, it will always return true.
     * If the key is added while contains is called, it may or may not return true.
     * Will always return false if the map does not contain the key.
     *
     * @param key
     * @return
     */
    public boolean containsKey(int key) {
        if (key == 0) {
            return zeroValue != null;
        }

        while (true) {
            try {
                for (Table t : new Table[]{expandedTable, table}) {
                    // Distinguish between foo and bar.
                    if (t != null && t.isAllocated()) {
                        TableResult r = t.get(key);
                        if (r == null) {
                            continue;
                        }
                        if (r.isBeingChanged()) {
                            throw new TryAgainException();
                        }
                        if (r.isDeleted()) {
                            return false;
                        } if (r.isSet() || r.isSetZero()) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (TryAgainException e) {
                // Try again!
            }
        }
    }

    /**
     * Returns the value associated with the specified key.
     * @param key
     */
    public int get(int key) {
        if (key == 0) {
            return zeroValue == null ? missingKeyValue : zeroValue;
        }

        while (true) {
            try {
                Table [] tables = new Table[]{expandedTable, table};
                for (Table t : tables) {
                    if (t == null || !t.isAllocated()) {
                        continue;
                    }
                    TableResult r = t.get(key);
                    if (r == null) {
                        // it's a miss!
                    } else if (r.isBeingChanged()) {
                        throw new TryAgainException();
                    } else if (r.isDeleted()) {
                        return missingKeyValue;
                    } else if (r.isSet()) {
                        return r.value;
                    }
                }
                return missingKeyValue;
            } catch (TryAgainException e) {
                // Try again!
            }
        }
    }


    /**
     * Adds the specified value to the keys.
     * @param value
     */
    public void put(int key, int value) {
        if (key == 0) {
            zeroValue = value;
            return;
        }

        byte newFlag = (value == 0) ? FLAG_SET_ZERO : FLAG_SET;
        int newValue = (value == 0) ? 1 : value;

        potentialNumElements.incrementAndGet();
        int resizeWork = 3;

        while (true) {
            expandIfNecessary();

            try {
                for (Table t : new Table[]{expandedTable, table}) {
                    if (t == null || !t.isAllocated()) {
                        continue;
                    }
                    while (t == expandedTable && 1.0 * potentialNumElements.get() / table.size() > Math.min(0.9, loadFactor * 1.4)) {
                        helpResize(3);
                    }
                    resizeWork -= helpResize(resizeWork);
                    TableResult r = t.getOrMake(key, value == 0 ? FLAG_SET_ZERO : FLAG_SET);
                    resizeWork -= helpResize(resizeWork);
                    if (r == null) {
                        throw new TryAgainException();  // out of space!
                    } else if (r.isFree()) {
                        if (!t.vals.compareAndSet(r.index, r.getEncodedValue(), newValue)) {
                            throw new IllegalStateException();  // we should own the lock!
                        }
                        numElements.incrementAndGet();
//                        if (expandedTable != null) System.err.print(" " + resizeWork);
                        return;
                    } else if (r.isSet()) {
                        potentialNumElements.decrementAndGet();
                        if (t.flags.compareAndSet(r.index, r.flag, newFlag)) {
                            t.vals.compareAndSet(r.index, r.getEncodedValue(), newValue);
                        }
//                        if (expandedTable != null) System.err.print(" " + resizeWork);
                        return;
                    } else if (r.isBeingChanged()) {
                        throw new TryAgainException();
                    } else if (r.isMoved()) {
                        throw new TryAgainException();
                    } else {
                        throw new IllegalStateException("Illegal state in put: " + r);
                    }
                }
            } catch (TryAgainException e) {
                // Try again!
            }

        }
    }

    /**
     * Returns the number of elements stored in the keys.
     * @return
     */
    public int size() {
        return numElements.get();
    }

    /**
     * Expand the underlying array if the load factor is exceeded.
     * If the load factor is NOT exceeded, no locking is required.
     * If it is exceeded, all threads block while one expands.
     */
    private void expandIfNecessary() {
        // Somebody else is expanding
        if (expandedTable != null) {
            return;
        }
        // Check if we're safe (usually the case, so no locks typically used!)
        if (potentialNumElements.get() < loadFactor * table.keys.length()) {
            return;
        }
        synchronized (numElements) {

            // Somebody else is expanding
            if (expandedTable != null) {
                return;
            }
            // Maybe somebody expanded while we were waiting for the lock
            if (potentialNumElements.get() < loadFactor * table.keys.length()) {
                return;
            }

            // calculate new size
            int newSize = (int) Math.ceil(table.keys.length() / loadFactor);
            if (1.0 * potentialNumElements.get() / newSize >= (loadFactor + 0.1)) {
                newSize *= 2;
            }

            LOG.fine("table expanding from " + table.size() + " to " + newSize + " with between " + potentialNumElements.get() + " elements ");
            resizeCounter = new ResizeCounter(table.keys.length());
            expandedTable = new Table(newSize);

            Thread t = new Thread("Expander") {
                @Override
                public void run() {
                    try {
                        helpResize(table.keys.length());
                        resizeCounter.waitForCompletion();
                        resizeCounter = null;
                        table = expandedTable;
                        expandedTable = null;
                        LOG.fine("finished expanding to " + table.size() + " with " + potentialNumElements + " potential elements");
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Expansion failed", e);
                        expandedTable = null;
                        resizeCounter = null;
                    }
                }
            };
            t.start();
        }
    }

    /**
     * Returns the number of elements actually resized
     * @param maxKeys
     * @return
     */
    private int helpResize(int maxKeys) {
        Table et = expandedTable;
        if (et == null || !et.isAllocated()) return 0;
        if (resizeCounter == null) return 0;
        int n = 0;
        for (int i = 0; i < maxKeys; i++) {
            ResizeCounter rc = resizeCounter;   // could change out from under us!
            if (rc == null) {
                break;
            }
            int j = rc.getWork();
            if (j < 0) {
                break;
            }
            try {
                while (!tryToMove(j)) {
                    // spin
                }
            } finally {
                rc.reportFinishedWork();
            }
            n++;
        }
        return n;
    }

    /**
     * Tries to move an index from the new table to the old one.
     * @param i
     * @return true iff the index was successfully moved.
     */
    private boolean tryToMove(int i) {
        byte f = table.flags.get(i);

        // If not set, try to mark it as moved.
        // If that fails, something must be in progress... try again
        if (f != FLAG_SET && f != FLAG_SET_ZERO) {
            return table.flags.compareAndSet(i, f, FLAG_MOVED);
        }
        int k = table.keys.get(i);
        int v = table.vals.get(i);
        TableResult r = new TableResult(i, k, v, f);
        if (r.isBeingChanged()) {
            return false;
        } else if (!r.isSet()) {
            throw new IllegalStateException("Unexpected state during expansion " + r);
        }

        // Make or obtain a bucket for the key
        TableResult r2 = expandedTable.getOrMake(k, FLAG_OLD);
        if (r2 == null) {
            throw new IllegalStateException("Out of space while expanding... EEK! new table size is " + expandedTable.size() + "; num elements is somewhere between " + numElements.get() + " and " + potentialNumElements.get());
        }

        // If somebody already stored the new key in the table it is up-to-date
        if (r2.flag != FLAG_UNSET) {
            table.flags.set(i, FLAG_MOVED);
            return true;
        }

        // If the store of the old key fails, something's in progress. Try again.
        if (!table.flags.compareAndSet(i, f, FLAG_MOVED)) {
            return false;
        }

        // We should have a lock, so the set of the flags and vals should succeed.
        if (!expandedTable.vals.compareAndSet(r2.index, 0, r.getEncodedValue())) {
            throw new IllegalStateException();
        }
        if (!expandedTable.flags.compareAndSet(r2.index, FLAG_OLD, r.getEncodedFlag())) {
            throw new IllegalStateException();
        }

        return true;
    }

    /**
     * Returns the keys.
     * This is a relatively expensive O(n) operation.
     * It will return all the elements in the keys at the start of the call, and
     * it may return any (or none of) the elements added while it is ongoing.
     *
     * @return the values in the keys.
     */
    public int[] keys() {
        TIntArrayList vals = new TIntArrayList();
        for (Table t : new Table[] { expandedTable, table}) {
            for (int i = 0; i < t.keys.length(); i++) {
                int f = t.flags.get(i);
                if (f == FLAG_SET) {
                    vals.add(t.keys.get(i));
                }
            }
        }

        return vals.toArray();
    }

    private int getActualCapacity(int desiredCapacity) {
        desiredCapacity = Math.max(16, desiredCapacity);
        int capacity = 1 << (32 - Integer.numberOfLeadingZeros(desiredCapacity - 1));
        if (capacity < desiredCapacity) {
            throw new IllegalStateException();
        }
        return capacity;
    }

    public class Table {
        private final int capacity;
        private volatile AtomicIntegerArray keys;
        private volatile AtomicByteArray flags;
        private volatile AtomicIntegerArray vals;

        public Table(int desiredCapacity) {
            this.capacity = getActualCapacity(Math.max(5, desiredCapacity));
            keys = new AtomicIntegerArray(capacity);
            flags = new AtomicByteArray(capacity);
            vals = new AtomicIntegerArray(capacity);
        }

        public boolean isAllocated() {
            return keys != null && flags != null && vals != null;
        }

        public TableResult get(int key) {
            if (vals == null) {
                throw new IllegalStateException();
            }
            int index = key;
            int length = keys.length();
            int startIndex = key & (length - 1);
            do {
                index &= (length - 1);
                int k = keys.get(index);
                if (k == 0) {
                    return null;
                } else if (k == key) {
                    return new TableResult(index, key, vals.get(index), flags.get(index));
                }
                index++;
            } while (index != startIndex);
            return null;
        }

        /**
         * Three possible cases:
         * 1. Key exists and is found.
         * 2. Key does not exist and is created.
         * 3. Key does not exist and we run out of space (i.e. return null).
         *
         * @param key
         * @param flag
         * @return
         */
        public TableResult getOrMake(int key, byte flag) {
            int index = key;
            int length = keys.length();
            int startIndex = key & (length - 1);

            int n = 0;
            do {
                index &= (length - 1);
//                System.out.println("probing " + index + " " + key);
                int k = keys.get(index);
                if (k == key) {
                    return new TableResult(index, key, vals.get(index), flags.get(index));
                } else if (k == 0) {
                    TableResult r = new TableResult(index, 0, vals.get(index), flags.get(index));
                    if (r.isFree()) {
                        // try to grab the slot
                        if (flags.compareAndSet(index, FLAG_UNSET, flag)) {
                            if (!keys.compareAndSet(index, 0, key)) {
                                throw new IllegalStateException();  // should have the lock!
                            }
                            return new TableResult(index, 0, vals.get(index), FLAG_UNSET);
                        }
                    }
                    // If not free, or the CAS failed, spin on this index; something's in progress!
                } else {
                    n++;
                    index++;
                }
                index &= (length - 1);
            } while (n == 0 || index != startIndex);
            return null;
        }

        public TableResult set(int key, int value) {
            TableResult r = getOrMake(key, value == 0 ? FLAG_SET_ZERO : FLAG_SET);
            if (r == null) {
                return null;
            }
            int newValue = (value == 0) ? 1 : value;
            vals.compareAndSet(r.index, r.getEncodedValue(), newValue);
            return r;
        }

        public int size() {
            return capacity;
        }
    }

    public static class TryAgainException extends RuntimeException {
    }

    public class ResizeCounter {
        final int length;
        final AtomicInteger counter = new AtomicInteger();
        final CountDownLatch latch;

        public ResizeCounter(int length) {
            this.length = length;
            this.latch  = new CountDownLatch(length);
        }

        public boolean hasMoreWork() {
            return counter.get() < length;
        }

        public int getWork() {
            int i = counter.getAndIncrement();
            return (i >= length) ? -1 : i;
        }

        public void reportFinishedWork() {
            latch.countDown();
        }

        public void waitForCompletion() throws InterruptedException {
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    public static class TableResult {
        public final int index;
        public final int key;
        public final Integer value;         // may be null
        public final byte flag;
        public boolean casResult;

        public TableResult(int index, int key, int value, byte flag) {
            this.index = index;
            this.key = key;
            this.flag = (byte) (flag & ~FLAG_VALUE_ZERO);
            if ((flag & FLAG_VALUE_ZERO) != 0) {
                this.value = 0;
            } else if (value == 0) {
                this.value = null;
            } else {
                this.value = value;
            }
        }

        public int getEncodedValue() {
            if (value == null) {
                return 0;
            } else if (value == 0) {
                return 1;   // set flag
            } else {
                return value;
            }
        }

        public byte getEncodedFlag() {
            if (value != null && value == 0 && flag == FLAG_SET) {
                return FLAG_SET_ZERO;
            } else {
                return flag;
            }
        }

        public boolean hasValue() {
            return value != null;
        }

        public boolean isFree() {
            return flag == 0 && value == null && key == 0;
        }

        public boolean isSet() {
            return flag == FLAG_SET && key != 0 && value != null;
        }

        public boolean isSetZero() {
            return flag == FLAG_SET_ZERO && key != 0 && value != null;
        }

        public boolean isBeingSet() {
            return flag == FLAG_SET && (key == 0 || value == null);
        }

        public boolean isDeleted() {
            return flag == FLAG_DELETED && value == 0;
        }

        public boolean isBeingDeleted() {
            return flag == FLAG_DELETED && value != 0;
        }

        public boolean isMoved() {
            return flag == FLAG_MOVED;
        }

        public boolean isBeingMoved() {
            return flag == FLAG_OLD;
        }

        public boolean isBeingChanged() {
            return isBeingSet() || isBeingDeleted() || isBeingMoved();
        }

        @Override
        public String toString() {
            return "TableResult{" +
                    "index=" + index +
                    ", key=" + key +
                    ", value=" + value +
                    ", flag=" + flag +
                    ", casResult=" + casResult +
                    '}';
        }
    }
}
