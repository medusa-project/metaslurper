package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;

import java.io.IOException;

/**
 * Encapsulates a remote content destination.
 */
public interface SinkService extends Service {

    /**
     * Closes all resources used by the service.
     */
    void close();

    /**
     * @param item Item to ingest.
     */
    void ingest(Item item) throws IOException;

}
