package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.GenericEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Source service whose {@link #numEntities()} method throws {@link
 * UnsupportedOperationException}.
 */
public class MockNonCountingSourceService extends MockSourceService
        implements SourceService {

    @Override
    public String getKey() {
        return MockNonCountingSourceService.class.getSimpleName().toLowerCase();
    }

    @Override
    public String getName() {
        return MockNonCountingSourceService.class.getSimpleName();
    }

    @Override
    public int numEntities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConcurrentIterator<Entity> entities() {
        final int numEntities = 2;

        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < numEntities; i++) {
            GenericEntity item = new GenericEntity();
            item.setSinkID("ID " + (i + 1));
            entities.add(item);
        }

        return new ConcurrentIterator<Entity>() {
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

}
