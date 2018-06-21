package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;

import java.io.IOException;

public class MockErroringSinkService extends MockSinkService
        implements SinkService {

    @Override
    public void ingest(ConcreteEntity entity) throws IOException {
        throw new IOException("I errored");
    }

}
