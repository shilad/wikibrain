import org.apache.commons.io.FileUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Shilad Sen
 */
public class BenchLevelDB {
    public static class RandomLongIterator implements Iterator<Long> {
        private final Random random = new Random();
        private int n;

        public RandomLongIterator(int n) {
            this.n = n;
        }

        @Override
        public boolean hasNext() {
            return n > 0;
        }

        @Override
        public Long next() {
            if (--n % 1000000 == 0) {
                System.err.println("doing " + n);
            }
            return random.nextLong() / 10000000 * 3921;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String args[]) throws InterruptedException {
        File foo = new File("foo");
        FileUtils.deleteQuietly(foo);
        DB db = DBMaker
            .newFileDB(foo)
            .mmapFileEnable()
            .transactionDisable()
            .asyncWriteEnable()
            .asyncWriteFlushDelay(100)
            .make();
        final int n = 160000000;
        final Set<Long> set = db.createTreeSet("foo")
                .pumpPresort(1000000)
                .pumpIgnoreDuplicates()
                .pumpSource(new RandomLongIterator(n))
                .make();
        final AtomicInteger hits = new AtomicInteger();
        final Random random = new Random();
        final AtomicInteger count = new AtomicInteger();
        final long before = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 8; i++) {
            threads.add(new Thread() {
                @Override
                public void run() {
                    while (true) {
                        int c = count.getAndIncrement();
                        if (c >= n) {
                            return;
                        }
                        if (c % 1000000 == 0) {
                            long tps = c / (System.currentTimeMillis() - before);
                            System.out.println("" + c + ", " + hits.get() + ", " + tps + " tx per milli");
                        }
                        long k = random.nextLong() / 10000000 * 3921;
                        if (set.contains(k)) {
                            hits.incrementAndGet();
                        }
                    }
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long after = System.currentTimeMillis();
        System.err.println("completed " + (1.0 * n / (after - before)) + " ops per milli");
    }
}
