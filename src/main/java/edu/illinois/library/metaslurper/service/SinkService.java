package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;

/**
 * Encapsulates a content destination.
 */
public interface SinkService extends Service {

    /**
     * Will be called before the first call to {@link #ingest(Entity)}.
     */
    void setNumEntitiesToIngest(int numEntitiesToIngest);

    /**
     * @param entity                 Entity to ingest.
     * @throws HarvestClosedException if the operation cannot be completed in
     *                               the current context.
     * @throws IOException           if there was some other error.
     */
    void ingest(Entity entity) throws IOException;

}
