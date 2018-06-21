package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.harvest.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockSinkService implements SinkService {

    private int numEntitiesToIngest;
    private final List<ConcreteEntity> ingestedEntities = new ArrayList<>();

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
    public void ingest(ConcreteEntity entity) throws IOException {
        ingestedEntities.add(entity);
    }

    public List<ConcreteEntity> getIngestedEntities() {
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
