package edu.illinois.library.metaslurper.service;

import java.util.Iterator;

/**
 * <p>Iterator designed for concurrent usage.</p>
 *
 * <p>Instead of a method like {@link Iterator#hasNext()} returning {@literal
 * false}, {@link #next()} throws an {@link EndOfIterationException} when
 * iteration is complete.</p>
 */
public interface ConcurrentIterator<T> {

    /**
     * Must be thread-safe.
     *
     * @return {@link T} The next element iterated.
     * @throws EndOfIterationException if iteration is complete.
     * @throws Exception if there was an error iterating.
     */
    T next() throws Exception;

}
