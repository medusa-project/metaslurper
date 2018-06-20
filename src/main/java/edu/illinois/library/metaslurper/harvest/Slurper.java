package edu.illinois.library.metaslurper.harvest;

import edu.illinois.library.metaslurper.Application;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class Slurper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slurper.class);

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
    public void slurp(final SourceService source, final SinkService sink) {
        slurp(source, sink, new Status());
    }

    /**
     * @param source Service to harvest.
     * @param sink   Service to harvest into.
     * @param status Object for status tracking.
     */
    void slurp(final SourceService source,
               final SinkService sink,
               final Status status) {
        final int numThreads       = Application.getNumThreads();
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        try {
            final int numEntities                 = source.numEntities();
            final AtomicInteger index             = new AtomicInteger();
            final ConcurrentIterator<Entity> iter = source.entities();

            status.setLifecycle(Lifecycle.RUNNING);
            sink.setNumEntitiesToIngest(numEntities);

            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> {
                    try {
                        while (true) { // will break on EndOfIterationException
                            final int currentIndex = index.getAndIncrement();
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
                                final Entity entity = iter.next();
                                if (entity != null) {
                                    sink.ingest(entity);
                                    status.incrementAndGetNumSucceeded();

                                    LOGGER.debug("Slurped {} {} from {} into {} [{}/{}] [{}]",
                                            entity.getVariant().name().toLowerCase(),
                                            entity, source, sink,
                                            currentIndex + 1, numEntities,
                                            percent(currentIndex + 1, numEntities));
                                } else {
                                    status.incrementAndGetNumFailed();
                                }
                            } catch (EndOfIterationException e) {
                                status.setLifecycle(Lifecycle.SUCCEEDED);
                                // If iteration has ended prematurely (based on
                                // numEntities), increment the failure count
                                // to make up the difference.
                                int delta = numEntities + 1 - index.get();
                                if (delta > 0) {
                                    status.addAndGetNumFailed(delta);
                                }
                                break;
                            } catch (HarvestClosedException e) {
                                int delta = numEntities + 1 - index.get();
                                if (delta > 0) {
                                    status.addAndGetNumFailed(delta);
                                }
                                status.setLifecycle(Lifecycle.ABORTED);
                                LOGGER.info("Harvest closed: {}", e.getMessage());
                                break;
                            } catch (Exception e) {
                                LOGGER.error("Failed to ingest into sink: {}",
                                        e.getMessage(), e);
                                status.incrementAndGetNumFailed();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            LOGGER.info("Slurping {} entities from {} into {} using {} threads",
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

}
