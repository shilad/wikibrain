package org.wikibrain.utils;

/**
 * Interface for a function that takes a single argument of type T  and returns nothing.
 *
 * @param <T>
 */
public interface Procedure<T> {
    /**
     * Call the function. If an exception occurs, it must be handled by the caller.
     * @param arg
     * @throws Exception
     */
    public void call(T arg) throws Exception;
}
