package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.harvest.Harvest;

/**
 * Fake service that doesn't do anything.
 */
final class TestSinkService implements SinkService {

    private static final String KEY = "test_sink";

    @Override
    public void close() {
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void ingest(ConcreteEntity entity) {
    }

    @Override
    public void setNumEntitiesToIngest(int numEntitiesToIngest) {
    }

    @Override
    public void setSourceKey(String sourceKey) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void updateHarvest(Harvest harvest) {
    }

}
