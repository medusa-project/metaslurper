package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.BasicEntity;
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
        BasicEntity item = new BasicEntity();
        item.setID("cats");
        item.setSourceURI("http://example.org/test");
        item.setServiceKey("test");
        item.getElements().add(new Element("title", "test"));
        instance.ingest(item);
    }

    @Test(expected = IOException.class)
    public void testIngestWithInvalidEntity() throws Exception {
        BasicEntity item = new BasicEntity();
        item.setID("cats");
        instance.ingest(item);
    }

    @Test
    public void testToString() {
        assertEquals(MetaslurpService.class.getSimpleName(),
                instance.toString());
    }

}
