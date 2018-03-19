package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class MockSinkService implements SinkService {

    private final List<Entity> ingestedEntities = new ArrayList<>();

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

}
