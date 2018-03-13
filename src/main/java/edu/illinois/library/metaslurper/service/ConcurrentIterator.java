package edu.illinois.library.metaslurper.service;

import java.util.Iterator;

/**
 * Iterator that works a little differently than {@link Iterator}, making it
 * better-suited to concurrent usage. Instead of a method like {@link
 * Iterator#hasNext()} returning {@literal false}, {@link #next()} simply
 * returns an instance of a type other than {@link T} as a signal to stop
 * iterating.
 */
public interface ConcurrentIterator<T> {

    /**
     * Must be thread-safe.
     *
     * @return {@link T} when returning an expected element, or an instance of
     *         some other class as a signal that iteration has concluded.
     */
    Object next();

}
