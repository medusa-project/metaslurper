package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.harvest.Status;

/**
 * Fake service that doesn't do anything.
 */
final class TestSinkService implements SinkService {

    @Override
    public void close() {
    }

    @Override
    public String getKey() {
        return "test";
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public void ingest(ConcreteEntity entity) {
    }

    @Override
    public void setNumEntitiesToIngest(int numEntitiesToIngest) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void updateStatus(Status status) {
    }

}
