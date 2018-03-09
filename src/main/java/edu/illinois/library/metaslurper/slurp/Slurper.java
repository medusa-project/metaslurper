package edu.illinois.library.metaslurper.slurp;

import edu.illinois.library.metaslurper.service.SourceService;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public final class Slurper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slurper.class);

    /**
     * Slurps all services.
     */
    public SlurpResult slurpAll() {
        final SlurpResult result = new SlurpResult(0, 0, Duration.ZERO);

        for (SourceService service : ServiceFactory.allServices()) {
            result.add(slurp(service));
            service.close();
        }
        return result;
    }

    /**
     * Slurps a single service.
     *
     * @param service Service to slurp.
     */
    public SlurpResult slurp(SourceService service) {
        final long start = System.currentTimeMillis();
        final int numItems = service.numItems();
        final AtomicInteger index = new AtomicInteger();
        final AtomicInteger numSucceeded = new AtomicInteger();
        final AtomicInteger numFailed = new AtomicInteger();

        service.items().forEach(it -> {
            if (it != null) {
                String msg = String.format("Consumed %s from %s (%d/%d; %.2f%%)",
                        it, service,
                        index.getAndIncrement(), numItems,
                        percent(numSucceeded.getAndIncrement(), numItems));
                LOGGER.debug(msg);
            } else {
                numFailed.incrementAndGet();
            }
        });

        final long end = System.currentTimeMillis();

        return new SlurpResult(numSucceeded.get(),
                numFailed.get(),
                Duration.ofMillis(end - start));
    }

    private float percent(int index, int total) {
        return (index / (float) total) * 100;
    }

}
