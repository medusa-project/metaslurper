package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.harvest.Harvest;
import edu.illinois.library.metaslurper.harvest.HarvestClosedException;
import edu.illinois.library.metaslurper.harvest.HTTPException;

import java.io.IOException;

/**
 * Encapsulates a content destination.
 */
public interface SinkService extends Service {

    /**
     * Called before the first call to {@link #ingest}.
     */
    void setNumEntitiesToIngest(int numEntitiesToIngest);

    /**
     * @param entity                  Entity to ingest.
     * @throws HarvestClosedException if the operation could not be completed
     *                                due to the harvest being closed, and no
     *                                more attempts should be made.
     * @throws HTTPException if there was an HTTP error.
     * @throws IOException if there was some other error.
     */
    void ingest(ConcreteEntity entity) throws IOException;

    /**
     * Sends a status update to the service.
     *
     * @param harvest Harvest to send. Implementations should not mutate it.
     * @throws HTTPException if there was an HTTP error.
     * @throws IOException if there was some other error.
     */
    void updateHarvest(Harvest harvest) throws IOException;

}
