package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;

/**
 * Encapsulates a remote source of content. Instances will connect to some
 * resource, typically a web server, and assemble and normalize content from
 * it.
 *
 * @author Alex Dolski UIUC
 */
public interface SourceService extends Service {

    /**
     * @return Number of entities publicly available in the service. Should be
     *         equal to the number of entities provided by {@link #entities()}.
     * @throws IOException if there is an error in obtaining the count.
     * @throws UnsupportedOperationException if finding the actual count is not
     *         possible or would be too burdensome.
     */
    int numEntities() throws IOException, UnsupportedOperationException;

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
     * @return Iterator of all entities to be harvested, which may be {@link
     *         edu.illinois.library.metaslurper.entity.ConcreteEntity}s for
     *         existing entities, or {@link
     *         edu.illinois.library.metaslurper.entity.PlaceholderEntity}s for
     *         missing ones&mdash;never {@literal null}.
     */
    ConcurrentIterator<? extends Entity> entities() throws IOException;

}
