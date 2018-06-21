package edu.illinois.library.metaslurper.harvest;

import edu.illinois.library.metaslurper.Application;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.PlaceholderEntity;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Alex Dolski UIUC
 */
public final class Harvester {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Harvester.class);

    /**
     * Update the sink status after any multiple of this many entities are
     * ingested.
     */
    private static final short STATUS_UPDATE_INCREMENT = 25;

    private static String percent(int numerator, int denominator) {
        if (denominator > 0) {
            return String.format("%.2f%%",
                    (numerator / (double) denominator) * 100);
        }
        return "?%";
    }

    /**
     * @param source Service to harvest.
     * @param sink   Service to harvest into.
     */
    public void harvest(final SourceService source, final SinkService sink) {
        harvest(source, sink, new Status());
    }

    /**
     * @param source Service to harvest.
     * @param sink   Service to harvest into.
     * @param status Object for status tracking.
     */
    void harvest(final SourceService source,
                 final SinkService sink,
                 final Status status) {
        final int numThreads       = Application.getNumThreads();
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        try {
            final int numEntities     = source.numEntities();
            final AtomicInteger index = new AtomicInteger();
            final ConcurrentIterator<? extends Entity> iter = source.entities();

            status.setLifecycle(Lifecycle.RUNNING);
            sink.setNumEntitiesToIngest(numEntities);

            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> {
                    try {
                        // Will break on EndOfIterationException or
                        // HarvestClosedException.
                        while (true) {
                            final int currentIndex = index.getAndIncrement();

                            // Update the harvest status, if necessary.
                            try {
                                if (currentIndex % STATUS_UPDATE_INCREMENT == 0
                                        && currentIndex + 1 < numEntities) {
                                    sink.updateStatus(status);
                                }
                            } catch (IOException e) {
                                LOGGER.error("Failed to update harvest status: {}",
                                        e.getMessage(), e);
                            }

                            try {
                                // Pull an Entity from the source service.
                                Entity entity = iter.next();

                                // Push it into the sink service.
                                if (entity instanceof ConcreteEntity) {
                                    ConcreteEntity concEntity = (ConcreteEntity) entity;
                                    try {
                                        sink.ingest(concEntity);
                                        status.incrementAndGetNumSucceeded();

                                        LOGGER.debug("Harvested {} {} from {} into {} [{}/{}] [{}]",
                                                concEntity.getVariant().name().toLowerCase(),
                                                concEntity, source, sink,
                                                currentIndex + 1, numEntities,
                                                percent(currentIndex + 1, numEntities));
                                    } catch (IOException e) {
                                        reportSinkFailure(status, concEntity, e);
                                    }
                                } else {
                                    reportSourceFailure(status,
                                            (PlaceholderEntity) entity);
                                }
                            } catch (EndOfIterationException e) {
                                // If iteration has ended prematurely,
                                // increment the failure count to make up the
                                // difference.
                                final int delta = numEntities + 1 - index.get();
                                if (delta > 0) {
                                    status.addAndGetNumFailed(delta);
                                    status.getMessages().add("Added " + delta +
                                            " to the failure count due to a " +
                                            "discrepancy between the number " +
                                            "of items reported present in " +
                                            "the service (" + numEntities +
                                            ") and the number found (" +
                                            (index.get() + 1) + ").");
                                }
                                status.setLifecycle(Lifecycle.SUCCEEDED);
                                break;
                            } catch (HarvestClosedException e) {
                                // Set the failure count to the number of
                                // items remaining.
                                final int delta = numEntities + 1 - index.get();
                                if (delta > 0) {
                                    status.addAndGetNumFailed(delta);
                                    status.getMessages().add("Harvest " +
                                            "aborted with " + delta +
                                            "items left.");
                                }
                                status.setLifecycle(Lifecycle.ABORTED);
                                LOGGER.info("Harvest closed: {}", e.getMessage());
                                break;
                            } catch (Exception e) {
                                reportSourceFailure(status, e);
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            LOGGER.info("Harvesting {} entities from {} into {} using {} threads",
                    numEntities, source, sink, numThreads);

            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.info(e.getMessage(), e);
            }
        } catch (IOException | UncheckedIOException e) {
            status.setLifecycle(Lifecycle.FAILED);
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                sink.updateStatus(status);
            } catch (IOException e) {
                LOGGER.error("Failed to update final harvest status: {}",
                        e.getMessage(), e);
            } finally {
                pool.shutdown();
            }
        }
    }

    /**
     * Reports a failure to acquire an {@link Entity} from a {@link
     * SourceService}.
     */
    private static void reportSourceFailure(Status status, Throwable t) {
        LOGGER.error("Failed to retrieve from source: {}", t.getMessage(), t);

        String message = String.format("**** SOURCE FAILURE:\n" +
                        "    Exception: %s\n" +
                        "    Message: %s\n" +
                        "    Stack Trace: %s\n",
                t.getClass().getSimpleName(),
                t.getMessage(),
                Arrays.stream(t.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n")));
        status.getMessages().add(message);
        status.incrementAndGetNumFailed();
    }

    /**
     * Reports a failure to acquire a {@link ConcreteEntity} from a {@link
     * SourceService}.
     */
    private static void reportSourceFailure(Status status,
                                            PlaceholderEntity entity) {
        String message = String.format("**** SOURCE FAILURE:\n" +
                        "    URI: %s\n" +
                        "    Source ID: %s\n",
                entity.getSourceURI(),
                entity.getSourceID());
        status.getMessages().add(message);
        status.incrementAndGetNumFailed();
    }

    /**
     * Reports a failure to ingest a {@link ConcreteEntity} into a {@link
     * SinkService}.
     */
    private static void reportSinkFailure(Status status,
                                          ConcreteEntity entity,
                                          Throwable t) {
        LOGGER.error("Failed to ingest into sink: {}", t.getMessage(), t);

        String message = String.format("**** SINK FAILURE:\n" +
                        "    URI: %s\n" +
                        "    Source ID: %s\n" +
                        "    Exception: %s\n" +
                        "    Stack Trace: %s\n",
                entity.getSourceURI(),
                entity.getSourceID(),
                t.getMessage(),
                Arrays.stream(t.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n")));
        status.getMessages().add(message);
        status.incrementAndGetNumFailed();
    }

}
