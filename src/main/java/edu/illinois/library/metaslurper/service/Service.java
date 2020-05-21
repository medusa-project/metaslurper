package edu.illinois.library.metaslurper.service;

/**
 * Abstract base interface for services.
 */
public interface Service extends AutoCloseable {

    /**
     * Closes all resources used by the instance, and stops all threads under
     * its management.
     */
    void close();

    /**
     * <p>Returns a key identifying the service that is unique among all other
     * services.</p>
     *
     * <p>This value is used by the command-line {@code -source} and
     * {@code -sink} arguments and may influence the "bucket" into which
     * content gets harvested at the sink end. It may be configurable in order
     * to support harvesting the same source content, using the same service
     * class, into different sink "buckets."</p>
     *
     * @return The service key.
     */
    String getKey();

    /**
     * The service name. This is typically used only for "pretty printing" log
     * messages.
     *
     * @return The service name.
     */
    String getName();

}
