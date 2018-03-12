package edu.illinois.library.metaslurper.slurp;

import edu.illinois.library.metaslurper.config.ConfigurationFactory;
import edu.illinois.library.metaslurper.entity.Item;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class Slurper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slurper.class);

    private static final int DEFAULT_NUM_THREADS = 1;

    private static int getNumThreads() {
        Configuration config = ConfigurationFactory.getConfiguration();
        return config.getInt("threads", DEFAULT_NUM_THREADS);
    }

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
    public SlurpResult slurpAll(SinkService sink) throws IOException {
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
                             SinkService sink) throws IOException {
        final long start                 = System.currentTimeMillis();
        final int numThreads             = getNumThreads();
        final int numItems               = source.numItems();
        final AtomicInteger index        = new AtomicInteger();
        final AtomicInteger numSucceeded = new AtomicInteger();
        final AtomicInteger numFailed    = new AtomicInteger();

        final Runnable slurper = () -> {
            try {
                Stream<Item> items = source.items();
                items = (numThreads > 1) ? items.parallel() : items;
                try {
                    items.forEach(item -> {
                        try {
                            if (item != null) {
                                try {
                                    sink.ingest(item);

                                    LOGGER.debug("Slurped {} from {} into {} [{}/{}] [{}]",
                                            item, source, sink, index.get(), numItems,
                                            percent(numSucceeded.incrementAndGet(), numItems));
                                } catch (IOException e) {
                                    LOGGER.warn("slurp(): {}", e.getMessage());
                                    numFailed.incrementAndGet();
                                }
                            } else {
                                numFailed.incrementAndGet();
                            }
                        } finally {
                            index.incrementAndGet();
                        }
                    });
                } finally {
                    items.close();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        LOGGER.info("Slurping {} items from {} into {} using {} threads",
                numItems, source, sink, numThreads);

        if (numThreads > 1) {
            final ForkJoinPool pool = new ForkJoinPool(numThreads);
            try {
                pool.submit(slurper).get(); // get() will block
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.info(e.getMessage(), e);
            }
        } else {
            slurper.run();
        }

        final long end = System.currentTimeMillis();

        return new SlurpResult(numSucceeded.get(),
                numFailed.get(),
                Duration.ofMillis(end - start));
    }

}
