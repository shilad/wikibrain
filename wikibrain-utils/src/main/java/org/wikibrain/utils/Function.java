package org.wikibrain.utils;

public interface Function<T,R> {
    /**
     * Call the function. If an exception occurs, it must be handled by the caller.
     * @param arg
     * @throws Exception
     */
    public R call(T arg) throws Exception;
}
