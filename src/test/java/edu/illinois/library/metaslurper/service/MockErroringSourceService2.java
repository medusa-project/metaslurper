package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;

import java.io.IOException;

/**
 * Source service whose {@link #items()} method throws an {@link IOException}.
 */
public class MockErroringSourceService2 extends MockSourceService
        implements SourceService {

    @Override
    public String getName() {
        return MockErroringSourceService2.class.getSimpleName();
    }

    @Override
    public ConcurrentIterator<Item> items() throws IOException {
        throw new IOException("I errored");
    }

}
