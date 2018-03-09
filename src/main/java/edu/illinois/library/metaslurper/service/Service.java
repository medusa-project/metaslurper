package edu.illinois.library.metaslurper.service;

/**
 * Abstract base interface for services.
 */
public interface Service {

    /**
     * @return The service name.
     */
    String getName();

    /**
     * Closes all resources used by the service.
     */
    void close();

}
