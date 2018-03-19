package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;

/**
 * Encapsulates a remote content destination.
 */
public interface SinkService extends Service {

    /**
     * @param entity Entity to ingest.
     */
    void ingest(Entity entity) throws IOException;

}
