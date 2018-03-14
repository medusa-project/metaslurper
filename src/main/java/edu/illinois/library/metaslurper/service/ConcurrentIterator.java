package edu.illinois.library.metaslurper.service;

import java.util.Iterator;

/**
 * <p>Iterator designed for concurrent usage.</p>
 *
 * <p>Instead of a method like {@link Iterator#hasNext()} returning {@literal
 * false}, {@link #next()} simply throws an exception when iteration is
 * complete.</p>
 */
public interface ConcurrentIterator<T> {

    /**
     * Must be thread-safe.
     *
     * @return {@link T} The next element iterated.
     * @throws EndOfIterationException when iteration is complete.
     */
    T next() throws EndOfIterationException;

}
