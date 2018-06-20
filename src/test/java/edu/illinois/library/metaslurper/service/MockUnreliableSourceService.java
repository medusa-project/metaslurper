package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.BasicEntity;
import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Source service whose {@link #entities()} method occasionally returns
 * {@literal null}.
 */
public class MockUnreliableSourceService extends MockSourceService
        implements SourceService {

    @Override
    public String getKey() {
        return MockUnreliableSourceService.class.getSimpleName().toLowerCase();
    }

    @Override
    public String getName() {
        return MockUnreliableSourceService.class.getSimpleName();
    }

    @Override
    public ConcurrentIterator<Entity> entities() throws IOException {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < numEntities(); i++) {
            BasicEntity item = new BasicEntity();
            item.setSinkID("ID " + (i + 1));
            entities.add(item);
        }

        return new ConcurrentIterator<Entity>() {
            private final int numEntities = numEntities();
            private final AtomicInteger index = new AtomicInteger();
            private final Iterator<Entity> it = entities.iterator();

            @Override
            public Entity next() throws EndOfIterationException {
                int i = index.getAndIncrement();
                if (i == 0) {
                    it.next();
                    return null; // very unreliable!
                } else if (i < numEntities) {
                    return it.next();
                } else {
                    throw new EndOfIterationException();
                }
            }
        };
    }

}
