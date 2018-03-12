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
     *         equal to the number of items provided by {@link #items()}.
     */
    int numItems() throws IOException;

    /**
     * <p>Provides a stream of all items publicly available in the service, in
     * an undefined order.</p>
     *
     * <p>If an item cannot be provided for some reason, it may be {@literal
     * null}.</p>
     *
     * <p>Implementations should try to be efficient and not try to load a
     * million results into memory.</p>
     *
     * <p>N.B.: Returned instances should be {@link Stream#close() closed}
     * after use.</p>
     *
     * @return Stream of items. The number of items in the stream is equal to
     *         {@link #numItems()}.
     */
    Stream<Item> items() throws IOException;

}
