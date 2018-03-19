package edu.illinois.library.metaslurper.slurp;

import edu.illinois.library.metaslurper.Application;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class Slurper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slurper.class);

    private static String percent(int numerator, int denominator) {
        if (denominator > 0) {
            return String.format("%.2f%%", (numerator / (float) denominator) * 100);
        }
        return "?%";
    }

    /**
     * Slurps all services.
     *
     * @param sink Service to slurp into.
     */
    public SlurpResult slurpAll(SinkService sink) {
        final SlurpResult result = new SlurpResult(0, 0, Duration.ZERO);

        for (SourceService service : ServiceFactory.allSourceServices()) {
            result.add(slurp(service, sink));
            service.close();
        }
        return result;
    }

    /**
     * Slurps a single service.
     *
     * @param source Service to slurp.
     * @param sink Service to slurp into.
     */
    public SlurpResult slurp(SourceService source,
                             SinkService sink) {
        final long start                 = System.currentTimeMillis();
        final AtomicInteger numSucceeded = new AtomicInteger();
        final AtomicInteger numFailed    = new AtomicInteger();
        final int numThreads             = Application.getNumThreads();
        final ExecutorService pool       = Executors.newFixedThreadPool(numThreads);
        try {
            final int numEntities                  = source.numEntities();
            final AtomicInteger index           = new AtomicInteger();
            final CountDownLatch latch          = new CountDownLatch(numThreads);
            final ConcurrentIterator<Entity> iter = source.entities();

            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> {
                    try {
                        while (true) {
                            try {
                                final Entity entity = iter.next();
                                if (entity != null) {
                                    sink.ingest(entity);
                                    numSucceeded.incrementAndGet();

                                    LOGGER.debug("Slurped {} from {} into {} [{}/{}] [{}]",
                                            entity, source, sink,
                                            index.get() + 1, numEntities,
                                            percent(index.get() + 1, numEntities));
                                } else {
                                    numFailed.incrementAndGet();
                                }
                            } catch (EndOfIterationException e) {
                                return;
                            } catch (IOException | RuntimeException e) {
                                LOGGER.error("slurp(): {}", e.getMessage(), e);
                                numFailed.incrementAndGet();
                            }
                            index.incrementAndGet();
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
            LOGGER.error(e.getMessage());
        } finally {
            pool.shutdown();
        }

        final long end = System.currentTimeMillis();

        return new SlurpResult(numSucceeded.get(), numFailed.get(),
                Duration.ofMillis(end - start));
    }

}
