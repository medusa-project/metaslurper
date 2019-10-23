package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.harvest.HTTPException;

import java.io.IOException;
import java.time.Instant;

/**
 * Encapsulates a remote source of content. Instances will connect to some
 * resource, typically but not necessarily a web server, and assemble and
 * normalize content from it.
 *
 * @author Alex Dolski UIUC
 */
public interface SourceService extends Service {

    /**
     * <p>Returns the number of entities publicly available in the service,
     * which is equal to the number of entities provided by {@link
     * #entities()}.</p>
     *
     * <p>If {@link #setLastModified(Instant)} did not throw an exception, the
     * count includes only entities last modified after the argument passed to
     * it.</p>
     *
     * @return See above.
     * @throws UnsupportedOperationException if obtaining a count is not
     *         possible or would be too burdensome.
     * @throws HTTPException if there is an HTTP error.
     * @throws IOException if there is some other error.
     */
    int numEntities() throws IOException, UnsupportedOperationException;

    /**
     * <p>Provides a thread-safe iterator of all {@link Entity entities}
     * publicly available in the service, in undefined order.</p>
     *
     * <p>If an entity cannot be provided during iteration for some reason, the
     * iterator returns a {@link
     * edu.illinois.library.metaslurper.entity.PlaceholderEntity} in its
     * place.</p>
     *
     * <p>If {@link #setLastModified(Instant)} did not throw an exception, the
     * only entities last modified after the argument passed to it are
     * iterated.</p>
     *
     * <p>Implementations should try to be efficient and not buffer a lot of
     * results in memory. They should also try to be resilient and recover from
     * errors.</p>
     *
     * @return Iterator of all entities to be harvested, which may be {@link
     *         edu.illinois.library.metaslurper.entity.ConcreteEntity}s for
     *         existing entities, or {@link
     *         edu.illinois.library.metaslurper.entity.PlaceholderEntity}s for
     *         missing ones&mdash;never {@literal null}.
     * @throws HTTPException if there was an HTTP error.
     * @throws IOException if there was some other error.
     */
    ConcurrentIterator<? extends Entity> entities() throws IOException;

    /**
     * Sets a last-modified date. Subsequent invocations of {@link
     * #numEntities()} and {@link #entities()} should omit any entities last
     * modified before it, if they are able.
     *
     * @param lastModified Last-modified time.
     */
    void setLastModified(Instant lastModified);

}
