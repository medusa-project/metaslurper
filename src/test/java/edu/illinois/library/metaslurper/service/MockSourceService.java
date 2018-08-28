package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.GenericEntity;
import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockSourceService implements SourceService {

    @Override
    public String getKey() {
        return MockSourceService.class.getSimpleName().toLowerCase();
    }

    @Override
    public String getName() {
        return MockSourceService.class.getSimpleName();
    }

    @Override
    public void close() {
    }

    @Override
    public int numEntities() throws IOException {
        return 2;
    }

    @Override
    public ConcurrentIterator<Entity> entities() throws IOException {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < numEntities(); i++) {
            GenericEntity item = new GenericEntity();
            item.setSinkID("ID " + (i + 1));
            entities.add(item);
        }

        return new ConcurrentIterator<Entity>() {
            private final int numEntities = numEntities();
            private final AtomicInteger index = new AtomicInteger();
            private final Iterator<Entity> it = entities.iterator();

            @Override
            public Entity next() throws EndOfIterationException {
                if (index.getAndIncrement() < numEntities) {
                    return it.next();
                } else {
                    throw new EndOfIterationException();
                }
            }
        };
    }

    @Override
    public void setLastModified(Instant lastModified)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
