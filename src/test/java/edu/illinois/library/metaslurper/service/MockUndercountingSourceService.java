package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.GenericEntity;
import edu.illinois.library.metaslurper.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Source service whose {@link #numEntities()} method returns a number smaller
 * than the number of entities available via its {@link #entities()} method.
 */
public class MockUndercountingSourceService extends MockSourceService
        implements SourceService {

    @Override
    public String getKey() {
        return MockUndercountingSourceService.class.getSimpleName().toLowerCase();
    }

    @Override
    public String getName() {
        return MockUndercountingSourceService.class.getSimpleName();
    }

    @Override
    public int numEntities() {
        return 2;
    }

    @Override
    public ConcurrentIterator<Entity> entities() {
        final int numEntities = numEntities() + 1;

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
