package edu.illinois.library.metaslurper.service;

import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulates a Metaslurp harvest.
 */
final class MetaslurpHarvest {

    enum Status {
        /**
         * The harvest is running.
         */
        RUNNING(1),

        /**
         * The harvest has been aborted.
         */
        ABORTED(2),

        /**
         * The harvest has completed successfully. This does not necessarily
         * mean that all items were ingested successfully.
         */
        SUCCEEDED(3),

        /**
         * The harvest encountered a fatal error and terminated prematurely.
         */
        FAILED(4);

        private int code;

        Status(int code) {
            this.code = code;
        }

        int getCode() {
            return code;
        }
    }

    private final AtomicInteger numEntities         = new AtomicInteger();
    private final AtomicInteger numIngestsSucceeded = new AtomicInteger();
    private final AtomicInteger numIngestsFailed    = new AtomicInteger();
    private URI endpointURI;
    private String key;
    private Status status = Status.RUNNING;

    MetaslurpHarvest(URI endpointURI, String key, int numEntities) {
        this.endpointURI = endpointURI;
        this.key = key;
        this.numEntities.set(numEntities);
    }

    String getKey() {
        return key;
    }

    int getNumEntities() {
        return numEntities.get();
    }

    int getNumIngestsSucceeded() {
        return numIngestsSucceeded.get();
    }

    int getNumIngestsFailed() {
        return numIngestsFailed.get();
    }

    synchronized Status getStatus() {
        return status;
    }

    URI getURI() {
        return endpointURI.resolve("/api/v1/harvests/" + getKey());
    }

    int incrementAndGetNumIngestsSucceeded() {
        return numIngestsSucceeded.incrementAndGet();
    }

    int incrementAndGetNumIngestsFailed() {
        return numIngestsFailed.incrementAndGet();
    }

    synchronized void setStatus(Status status) {
        this.status = status;
    }

    String toJSON() {
        JSONObject jobj = new JSONObject();
        jobj.put("status", getStatus().getCode());
        jobj.put("num_items", numEntities.get());
        jobj.put("num_succeeded", numIngestsSucceeded.get());
        jobj.put("num_failed", numIngestsFailed.get());
        return jobj.toString();
    }

    @Override
    public String toString() {
        return key;
    }

}
