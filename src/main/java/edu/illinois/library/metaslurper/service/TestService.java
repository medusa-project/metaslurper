package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.GenericEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Fake service that supplies some fake content.
 */
final class TestService implements SourceService {

    private static final List<Entity> ENTITIES = new ArrayList<>();
    private static final String KEY = "test";

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
        final Iterator<Entity> it = ENTITIES.iterator();

        return new ConcurrentIterator<Entity>() {
            @Override
            public synchronized Entity next() throws Exception {
                if (it.hasNext()) {
                    return it.next();
                } else {
                    throw new EndOfIterationException();
                }
            }
        };
    }

    @Override
    public int numEntities() throws UnsupportedOperationException {
        return ENTITIES.size();
    }

    @Override
    public String toString() {
        return getKey();
    }

}
