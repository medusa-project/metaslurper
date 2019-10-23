package edu.illinois.library.metaslurper.harvest;

import edu.illinois.library.metaslurper.Application;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.PlaceholderEntity;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import edu.illinois.library.metaslurper.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Alex Dolski UIUC
 */
public final class Harvester {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Harvester.class);

    /**
     * Update the sink status after a multiple of this many entities are
     * ingested.
     */
    private static final short STATUS_UPDATE_INCREMENT = 25;

    /**
     * @param source Service to harvest.
     * @param sink   Service to harvest into.
     */
    public void harvest(final SourceService source, final SinkService sink) {
        harvest(source, sink, new Harvest());
    }

    /**
     * @param source  Service to harvest.
     * @param sink    Service to harvest into.
     * @param harvest Object for status tracking.
     */
    void harvest(final SourceService source,
                 final SinkService sink,
                 final Harvest harvest) {
        final int numThreads       = Application.getNumThreads();
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        try {
            final int numEntities                         = getNumEntities(source);
            final ConcurrentIterator<? extends Entity> it = source.entities();

            harvest.setLifecycle(Lifecycle.RUNNING);
            harvest.setNumEntities(numEntities);
            sink.setNumEntitiesToIngest(numEntities);

            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> harvestInThread(harvest, source, sink, it,
                        latch));
            }

            LOGGER.info("Harvesting {} entities from {} into {} using {} threads",
                    numEntities, source, sink, numThreads);

            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.info(e.getMessage(), e);
            }
        } catch (IOException | UncheckedIOException e) {
            harvest.setLifecycle(Lifecycle.FAILED);
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                sink.updateHarvest(harvest);
            } catch (IOException e) {
                LOGGER.error("Failed to update final harvest status: {}",
                        e.getMessage(), e);
            } finally {
                pool.shutdown();
            }
        }
    }

    private void harvestInThread(Harvest harvest,
                                 SourceService source,
                                 SinkService sink,
                                 ConcurrentIterator<? extends Entity> it,
                                 CountDownLatch latch) {
        try {
            // Will break on an EndOfIterationException or
            // HarvestClosedException.
            while (throttle()) {
                final int index = harvest.getAndIncrementIndex();
                updateStatus(sink, harvest);
                try {
                    // Pull an Entity from the source service.
                    Entity entity = it.next();

                    // Push it into the sink service.
                    if (entity instanceof ConcreteEntity) {
                        ConcreteEntity concEntity = (ConcreteEntity) entity;
                        try {
                            sink.ingest(concEntity);
                            harvest.incrementNumSucceeded();

                            LOGGER.debug("Harvested {} {} from {} into {} [{}/{}] [{}]",
                                    concEntity.getVariant().name().toLowerCase(),
                                    concEntity, source, sink,
                                    index + 1, harvest.getNumEntities(),
                                    NumberUtils.percent(index + 1, harvest.getNumEntities()));
                        } catch (HarvestClosedException e) {
                            throw e;
                        } catch (IOException e) {
                            reportSinkError(harvest, concEntity, e);
                        }
                    } else {
                        reportSourceError(harvest,
                                (PlaceholderEntity) entity);
                    }
                } catch (EndOfIterationException e) {
                    harvest.endPrematurely();
                    LOGGER.info("Harvest ended prematurely: {}", e.getMessage());
                    break;
                } catch (HarvestClosedException e) {
                    harvest.abort();
                    LOGGER.info("Harvest closed: {}", e.getMessage());
                    break;
                } catch (Exception e) {
                    reportSourceError(harvest, e);
                }
            }
        } finally {
            latch.countDown();
        }
    }

    private static void updateStatus(SinkService sink,
                                     Harvest harvest) {
        try {
            if (harvest.getIndex() % STATUS_UPDATE_INCREMENT == 0) {
                sink.updateHarvest(harvest);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to update harvest status: {}",
                    e.getMessage(), e);
        }
    }

    private static int getNumEntities(SourceService source) throws IOException {
        int numEntities;
        try {
            numEntities = source.numEntities();
        } catch (UnsupportedOperationException e) {
            numEntities = 0;
        }
        return numEntities;
    }

    /**
     * @return {@code} true.
     */
    private static boolean throttle() {
        try {
            Thread.sleep(Application.getThrottleMsec());
        } catch (InterruptedException ignore) {
        }
        return true;
    }

    /**
     * Reports a failure to acquire an {@link Entity} from a {@link
     * SourceService}.
     */
    private static void reportSourceError(Harvest harvest, Throwable t) {
        LOGGER.error("Failed to retrieve from source: {}", t.getMessage(), t);

        final List<String> lines = new ArrayList<>();
        lines.add("******** SOURCE ERROR ********");
        lines.add("Time: " + Instant.now());
        lines.add(getMessage(t));

        String message = String.join("\n", lines) + "\n";
        harvest.addMessage(message);
        harvest.incrementNumFailed();
    }

    /**
     * Reports a failure to acquire a {@link ConcreteEntity} from a {@link
     * SourceService}; i.e. the iterator returned by {@link
     * SourceService#entities()} has returned a {@link PlaceholderEntity}.
     */
    private static void reportSourceError(Harvest harvest,
                                          PlaceholderEntity entity) {
        String message = String.format(
                "******** SOURCE ERROR ********\n" +
                        "Time: %s\n" +
                        "URI: %s\n" +
                        "Source ID: %s\n",
                Instant.now(),
                entity.getSourceURI(),
                entity.getSourceID());
        harvest.addMessage(message);
        harvest.incrementNumFailed();
    }

    /**
     * Reports a failure to ingest a {@link ConcreteEntity} into a {@link
     * SinkService}.
     */
    private static void reportSinkError(Harvest harvest,
                                        ConcreteEntity entity,
                                        Throwable t) {
        LOGGER.error("Failed to ingest into sink: {}", t.getMessage(), t);

        final List<String> lines = new ArrayList<>();
        lines.add("******** SINK ERROR ********");
        lines.add("Time: " + Instant.now());
        lines.add("Source URI: " + entity.getSourceURI());
        lines.add("Source ID: " + entity.getSourceID());
        lines.add(getMessage(t));

        String message = String.join("\n", lines) + "\n";
        harvest.addMessage(message);
        harvest.incrementNumFailed();
    }

    private static String getMessage(Throwable t) {
        final List<String> lines = new ArrayList<>();
        lines.add("Exception: " + t.getClass().getName());
        lines.add("\tMessage: " + t.getMessage());
        lines.add("\tStack Trace: " + Arrays.stream(t.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n\t\t\t")));
        // If the Throwable is an HTTPException, we can wring some more useful
        // information out of it.
        if (t instanceof HTTPException) {
            HTTPException hte = (HTTPException) t;
            lines.add("Method: " + hte.getMethod());
            lines.add("URI: " + hte.getURI());
            hte.getStatusCode().ifPresent(code ->
                    lines.add("Status: " + code));
            hte.getRequestBody().ifPresent(body ->
                    lines.add("Request body: " + body));
            hte.getResponseBody().ifPresent(body ->
                    lines.add("Response body: " + body));
        }
        return String.join("\n", lines);
    }

}
