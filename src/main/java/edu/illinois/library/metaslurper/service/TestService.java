package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.GenericEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake service that supplies some fake content.
 */
final class TestService implements SourceService {

    private static final List<Entity> ENTITIES = new ArrayList<>();
    private static final String KEY = "test";

    private Instant lastModified;

    static {
        // add an item
        GenericEntity e = new GenericEntity();
        e.setSourceID("1");
        e.setServiceKey(KEY);
        e.setSourceURI("http://example.net/" + e.getSourceID());
        e.setSinkID(KEY + "-" + e.getSourceID());
        e.setVariant(Variant.ITEM);
        e.getElements().add(new Element("title", "Test Item"));
        ENTITIES.add(e);

        // add a collection
        e = new GenericEntity();
        e.setSourceID("2");
        e.setServiceKey(KEY);
        e.setSourceURI("http://example.net/" + e.getSourceID());
        e.setSinkID(KEY + "-" + e.getSourceID());
        e.setVariant(Variant.COLLECTION);
        e.getElements().add(new Element("title", "Test Collection"));
        ENTITIES.add(e);
    }

    @Override
    public void close() {}

    @Override
    public String getKey() {
        return "test";
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public ConcurrentIterator<Entity> entities() {
        final int count = numEntities();
        final AtomicInteger index = new AtomicInteger(0);

        return () -> {
            final int i = index.getAndIncrement();
            if (i < count) {
                return ENTITIES.get(i);
            } else {
                throw new EndOfIterationException();
            }
        };
    }

    @Override
    public int numEntities() throws UnsupportedOperationException {
        // If lastModified is set, we'll say half of the entities were last
        // modified after it.
        return (lastModified != null) ?
                Math.round(ENTITIES.size() / 2f) : ENTITIES.size();
    }

    @Override
    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return getKey();
    }

}
