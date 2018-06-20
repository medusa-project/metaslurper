package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.slurp.HarvestClosedException;
import edu.illinois.library.metaslurper.slurp.Status;

import java.io.IOException;

/**
 * Encapsulates a content destination.
 */
public interface SinkService extends Service {

    /**
     * Called before the first call to {@link #ingest(Entity)}.
     */
    void setNumEntitiesToIngest(int numEntitiesToIngest);

    /**
     * @param entity                  Entity to ingest.
     * @throws HarvestClosedException if the operation could not be completed
     *                                due to the harvest being closed, and no
     *                                more attempts should be made.
     * @throws IOException            if there was some other error.
     */
    void ingest(Entity entity) throws IOException;

    /**
     * Sends a status update to the service.
     *
     * @param status       Status to send. Implementations should not mutate
     *                     it.
     * @throws IOException if there was an error.
     */
    void updateStatus(Status status) throws IOException;

}
