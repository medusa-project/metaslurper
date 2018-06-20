package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.slurp.Status;

import java.util.ArrayList;
import java.util.List;

public class MockSinkService implements SinkService {

    private int numEntitiesToIngest;
    private final List<Entity> ingestedEntities = new ArrayList<>();

    @Override
    public String getKey() {
        return MockSinkService.class.getSimpleName().toLowerCase();
    }

    @Override
    public String getName() {
        return MockSinkService.class.getSimpleName();
    }

    @Override
    public void close() {
    }

    @Override
    public void ingest(Entity entity) {
        ingestedEntities.add(entity);
    }

    public List<Entity> getIngestedEntities() {
        return ingestedEntities;
    }

    @Override
    public void setNumEntitiesToIngest(int numEntitiesToIngest) {
        this.numEntitiesToIngest = numEntitiesToIngest;
    }

    @Override
    public void updateStatus(Status stats) {
    }

}
