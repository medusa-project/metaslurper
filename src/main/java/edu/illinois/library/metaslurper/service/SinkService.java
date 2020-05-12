package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.harvest.Harvest;
import edu.illinois.library.metaslurper.harvest.HarvestClosedException;

import java.io.IOException;

/**
 * Encapsulates a content destination.
 */
public interface SinkService extends Service {

    /**
     * Called before the first invocation of {@link #ingest}.
     */
    void setNumEntitiesToIngest(int numEntitiesToIngest);

    /**
     * <p>Called after {@link #setNumEntitiesToIngest(int)} but before the
     * first invocation (if any) of {@link #ingest(ConcreteEntity)}.</p>
     *
     * <p>This is not necessarily a simple setter method. Note that {@link
     * #ingest(ConcreteEntity)} may not get called at all (in the case of a
     * zero-entity harvest) so this would be a good time to initialize a {@link
     * Harvest}.</p>
     *
     * @param sourceKey Key of the {@link SourceService} being ingested.
     * @throws IOException upon error.
     */
    void setSourceKey(String sourceKey) throws IOException;

    /**
     * Invoked once for each entity to be ingested into the sink service. Note
     * that invocations may occur from different threads. Also, an invocation
     * of this method may not occur at all (in the case of a harvest of zero
     * entities).
     *
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
