package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.harvest.Status;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulates a {@link MetaslurpService} harvest.
 */
final class MetaslurpHarvest {

    private final AtomicInteger numEntities = new AtomicInteger();
    private String key;
    private Status status;

    MetaslurpHarvest(String key, int numEntities) {
        this.key = key;
        this.numEntities.set(numEntities);
    }

    String getKey() {
        return key;
    }

    /**
     * @return Number of entities to harvest.
     */
    int getNumEntities() {
        return numEntities.get();
    }

    /**
     * @return URI path.
     */
    String getPath() {
        return "/api/v1/harvests/" + getKey();
    }

    private int getStatusCode() {
        switch (status.getLifecycle()) {
            case NEW:
                return 0;
            case RUNNING:
                return 1;
            case ABORTED:
                return 2;
            case SUCCEEDED:
                return 3;
            case FAILED:
                return 4;
            default:
                throw new IllegalArgumentException(
                        "Unrecognized lifecycle: " + status.getLifecycle());
        }
    }

    void setStatus(Status status) {
        this.status = status;
    }

    String toJSON() {
        JSONObject jobj = new JSONObject();
        jobj.put("status", getStatusCode());
        jobj.put("num_items", numEntities.get());
        jobj.put("num_succeeded", status.getNumSucceeded());
        jobj.put("num_failed", status.getNumFailed());
        JSONArray jmessages = new JSONArray();
        for (String message : status.getMessages()) {
            jmessages.put(message);
        }
        jobj.put("messages", jmessages);
        return jobj.toString();
    }

    @Override
    public String toString() {
        return key;
    }

}
