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
     * @throws EndOfIterationException when iteration is complete.
     * @throws IterationException when the instance is no longer iterable due
     *         to an error.
     */
    T next() throws EndOfIterationException, IterationException;

}
