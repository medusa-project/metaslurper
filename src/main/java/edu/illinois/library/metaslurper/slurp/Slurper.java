package edu.illinois.library.metaslurper.slurp;

import edu.illinois.library.metaslurper.async.ThreadPool;
import edu.illinois.library.metaslurper.entity.Item;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Slurper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slurper.class);

    private final BlockingQueue<Item> sinkQueue = new LinkedBlockingQueue<>();

    private static float percent(int index, int total) {
        return (index / (float) total) * 100;
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
    public SlurpResult slurp(SourceService source, SinkService sink) {
        final long start                 = System.currentTimeMillis();
        final int numItems               = source.numItems();
        final AtomicInteger index        = new AtomicInteger();
        final AtomicInteger numSucceeded = new AtomicInteger();
        final AtomicInteger numFailed    = new AtomicInteger();

        // Rather than ingesting items into the sink in the main thread, we
        // will add them to a queue which is consumed in another thread.
        ThreadPool.getInstance().submit(() -> {
            while (index.get() < numItems) {
                try {
                    Item item = sinkQueue.take();
                    sink.ingest(item);

                    LOGGER.debug(String.format("Ingested %s into %s [%d/%d] [%.2f%%]",
                            item, sink,
                            index.getAndIncrement(), numItems,
                            percent(numSucceeded.incrementAndGet(), numItems)));
                } catch (IOException e) {
                    LOGGER.warn("slurp(): {}", e.getMessage());
                    numFailed.incrementAndGet();
                } catch (InterruptedException ignore) {
                    numFailed.incrementAndGet();
                }
            }
        });

        source.items().parallel().forEach(item -> {
            try {
                if (item != null) {
                    LOGGER.debug("Slurped {} from {}", item, source);
                    sinkQueue.add(item);
                } else {
                    numFailed.incrementAndGet();
                }
            } finally {
                index.incrementAndGet();
            }
        });

        final long end = System.currentTimeMillis();

        return new SlurpResult(numSucceeded.get(),
                numFailed.get(),
                Duration.ofMillis(end - start));
    }

}
