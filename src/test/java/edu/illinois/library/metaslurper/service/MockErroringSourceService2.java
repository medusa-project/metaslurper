package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;

/**
 * Source service whose {@link #entities()} method throws an {@link IOException}.
 */
public class MockErroringSourceService2 extends MockSourceService
        implements SourceService {

    @Override
    public String getName() {
        return MockErroringSourceService2.class.getSimpleName();
    }

    @Override
    public ConcurrentIterator<Entity> entities() throws IOException {
        throw new IOException("I errored");
    }

}
