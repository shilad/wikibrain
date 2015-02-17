package org.wikibrain.utils;

public interface Procedure<T> {
    /**
     * Call the function. If an exception occurs, it must be handled by the caller.
     * @param arg
     * @throws Exception
     */
    public void call(T arg) throws Exception;
}
