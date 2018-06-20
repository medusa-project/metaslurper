package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.harvest.HarvestClosedException;

public class MockAbortingSourceService extends MockSourceService
        implements SourceService {

    @Override
    public String getKey() {
        return MockAbortingSourceService.class.getSimpleName().toLowerCase();
    }

    @Override
    public String getName() {
        return MockAbortingSourceService.class.getSimpleName();
    }

    @Override
    public ConcurrentIterator<Entity> entities() {
        return () -> {
            throw new HarvestClosedException();
        };
    }

}
