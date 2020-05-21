package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.GenericEntity;
import edu.illinois.library.metaslurper.entity.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MetaslurpServiceTest {

    private MetaslurpService instance;

    @Before
    public void setUp() {
        instance = new MetaslurpService();
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test
    public void testIngestWithValidEntity() throws Exception {
        try {
            GenericEntity item = new GenericEntity();
            item.setSinkID("cats");
            item.setSourceID("cats");
            item.setSourceURI("http://example.org/test");
            item.setServiceKey("test");
            item.getElements().add(new Element("title", "test"));
            instance.setNumEntitiesToIngest(1);
            instance.setSourceKey("test");
            instance.ingest(item);
        } finally {
            instance.deleteHarvest();
        }
    }

    @Test(expected = IOException.class)
    public void testIngestWithInvalidEntity() throws Exception {
        try {
            GenericEntity item = new GenericEntity();
            item.setSinkID("cats");
            instance.setNumEntitiesToIngest(1);
            instance.setSourceKey("test");
            instance.ingest(item);
        } finally {
            instance.deleteHarvest();
        }
    }

    @Test
    public void testToString() {
        assertEquals(MetaslurpService.class.getSimpleName(),
                instance.toString());
    }

}
