package edu.illinois.library.metaslurper.service;

import java.io.IOException;

/**
 * Source service whose {@link #numEntities()} method throws an
 * {@link IOException}.
 */
public class MockErroringSourceService1 extends MockSourceService
        implements SourceService {

    @Override
    public String getName() {
        return MockErroringSourceService1.class.getSimpleName();
    }

    @Override
    public int numEntities() throws IOException {
        throw new IOException("I errored");
    }

}
