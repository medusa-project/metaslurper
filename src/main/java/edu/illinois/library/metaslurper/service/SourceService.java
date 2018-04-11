package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;

/**
 * Encapsulates a remote source of content. Instances will connect to some
 * resource, typically a web server, and assemble and normalize content from
 * it.
 */
public interface SourceService extends Service {

    /**
     * @return Number of entities publicly available in the service. Should be
     *         equal to the number of entities provided by {@link #entities()}.
     *         May be a negative number if finding the actual count would be
     *         too burdensome.
     */
    int numEntities() throws IOException;

    /**
     * <p>Provides a thread-safe iterator of all entities publicly available in
     * the service, in undefined order.</p>
     *
     * <p>If an entity cannot be provided for some reason, it may be {@literal
     * null}.</p>
     *
     * <p>Implementations should try to be efficient and not load a lot of
     * results into memory.</p>
     *
     * <p>They should also try to be resilient and recover from errors.</p>
     *
     * <p>If {@link #numEntities()} returns a non-negative value, the number of
     * entities iterated (whether or not they are {@literal null}) should be
     * equal to that.</p>
     *
     * @return Iterator of entities.
     */
    ConcurrentIterator<Entity> entities() throws IOException;

}
