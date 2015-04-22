package org.wikibrain.utils;

/**
 * Interface for a function that takes a single argument of type T
 * and returns a value of type R.
 *
 * @param <T>
 * @param <R>
 */
public interface Function<T,R> {
    /**
     * Call the function. If an exception occurs, it must be handled by the caller.
     * @param arg
     * @throws Exception
     */
    public R call(T arg) throws Exception;
}
