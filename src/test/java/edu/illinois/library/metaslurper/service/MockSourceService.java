package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.BasicEntity;
import edu.illinois.library.metaslurper.entity.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockSourceService implements SourceService {

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
            BasicEntity item = new BasicEntity();
            item.setID("ID " + (i + 1));
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

}
