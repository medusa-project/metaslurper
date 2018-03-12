package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Encapsulates a remote source of content. Instances will connect to some
 * resource, typically a web server, and assemble and normalize content from
 * it.
 */
public interface SourceService extends Service {

    /**
     * @return Number of items publicly available in the service. Should be
     *         equal to the number of items provided by {@link #items()}. May
     *         be a negative number if calculating the number of items would
     *         be too burdensome.
     */
    int numItems() throws IOException;

    /**
     * <p>Provides a stream of all items publicly available in the service, in
     * an undefined order.</p>
     *
     * <p>If an item cannot be provided for some reason, it may be {@literal
     * null}.</p>
     *
     * <p>Implementations should try to be efficient and not load a lot of
     * results into memory.</p>
     *
     * <p>If {@link #numItems()} returns a non-negative value, the number of
     * items in the stream (whether or not they are {@literal null}) should be
     * equal to that.</p>
     *
     * <p>N.B.: Returned instances should be {@link Stream#close() closed}
     * after use.</p>
     *
     * @return Stream of items.
     */
    Stream<Item> items() throws IOException;

}
