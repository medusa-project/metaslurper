package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

/**
 * Source service whose {@link #items()} method returns a stream that throws an
 * {@link UncheckedIOException}.
 */
public class MockErroringSourceService3 extends MockSourceService
        implements SourceService {

    @Override
    public String getName() {
        return MockErroringSourceService3.class.getSimpleName();
    }

    @Override
    public Stream<Item> items() {
        return Stream.generate(() -> {
            throw new UncheckedIOException(new IOException("I errored"));
        });
    }

}
