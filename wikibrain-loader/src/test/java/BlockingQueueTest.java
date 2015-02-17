import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Shilad Sen
 */
public class BlockingQueueTest {

    // 10M
    private static int NUM_ENTRIES = 10000000;
    private static int NUM_THREADS = 30;
    private static final AtomicBoolean finished = new AtomicBoolean(false);
    private static final BlockingQueue queue = new ArrayBlockingQueue(1000);


    public static void main(String args[]) throws InterruptedException {
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < NUM_ENTRIES; i++) {
                        queue.put(new Integer(i));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                finished.set(true);
            }
        });

        List<Thread> consumers = new ArrayList<Thread>();
        for (int i = 0; i < NUM_THREADS; i++) {
            consumers.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    int n = 0;
                    try {
                        while (!finished.get()) {
                            Integer i = (Integer) queue.poll(100, TimeUnit.MILLISECONDS);
                            n++;
                        }
                    } catch (InterruptedException e) {
                        System.err.println("read " + n);
                    }
                }
            }));
        }

        long begin = System.currentTimeMillis();
        producer.start();
        for (Thread r : consumers) {
            r.start();
        }
        producer.join();
        System.err.println("DONE!");
        for (Thread r : consumers) {
            r.join();
        }
        long end = System.currentTimeMillis();
        System.out.println("elapsed time is " + ((end - begin) / 1000.0) + " seconds");
    }
}
