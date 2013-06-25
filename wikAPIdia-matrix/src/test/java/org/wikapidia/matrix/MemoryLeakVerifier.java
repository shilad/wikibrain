package org.wikapidia.matrix;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertNull;

/**
 * A simple utility class that can verify that an object has been successfully garbage collected.
 */
public class MemoryLeakVerifier {
    private static final int MAX_GC_ITERATIONS = 50;
    private static final int GC_SLEEP_TIME     = 100;

    private final WeakReference reference;

    public MemoryLeakVerifier(Object object) {
        this.reference = new WeakReference(object);
    }

    public Object getObject() {
        return reference.get();
    }

    /**
     * Attempts to perform a full garbage collection so that all weak references will be removed. Usually only
     * a single GC is required, but there have been situations where some unused memory is not cleared up on the
     * first pass. This method performs a full garbage collection and then validates that the weak reference
     * now has been cleared. If it hasn't then the thread will sleep for 50 milliseconds and then retry up to
     * 10 more times. If after this the object still has not been collected then the assertion will fail.
     *
     * Based upon the method described in: http://www.javaworld.com/javaworld/javatips/jw-javatip130.html
     */
    public void assertGarbageCollected(String name) {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < MAX_GC_ITERATIONS; i++) {
            runtime.runFinalization();
            runtime.gc();
            if (getObject() == null)
                break;

            // Pause for a while and then go back around the loop to try again...
            try {
                EventQueue.invokeAndWait(new Runnable() {public void run() {}});
                Thread.sleep(GC_SLEEP_TIME);
            } catch (InterruptedException e) {
                // Ignore any interrupts and just try again...
            } catch (InvocationTargetException e) {
                // Ignore any interrupts and just try again...
            }
        }
        assertNull(name + ": object should not exist after " + MAX_GC_ITERATIONS + " collections", getObject());
    }
}