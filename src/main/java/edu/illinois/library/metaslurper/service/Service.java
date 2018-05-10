package edu.illinois.library.metaslurper.service;

/**
 * Abstract base interface for services.
 */
public interface Service extends AutoCloseable {

    /**
     * @return The service name.
     */
    String getName();

    /**
     * Closes all resources used by the service, and stops all of its threads.
     */
    void close();

}
