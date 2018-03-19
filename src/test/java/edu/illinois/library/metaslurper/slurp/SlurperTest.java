package edu.illinois.library.metaslurper.slurp;

import edu.illinois.library.metaslurper.service.MockErroringSourceService1;
import edu.illinois.library.metaslurper.service.MockErroringSourceService2;
import edu.illinois.library.metaslurper.service.MockSinkService;
import edu.illinois.library.metaslurper.service.MockSourceService;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

public class SlurperTest {

    private Slurper instance;

    @Before
    public void setUp() {
        instance = new Slurper();
    }

    @Test
    public void testSlurpAll() throws Exception {
        MockSourceService source = new MockSourceService();
        MockSinkService sink = new MockSinkService();

        ServiceFactory.setSourceServices(
                new HashSet<>(Arrays.asList(source)));
        ServiceFactory.setSinkServices(
                new HashSet<>(Collections.singletonList(sink)));

        try {
            SlurpResult result = instance.slurpAll(sink);
            assertEquals(source.numEntities(), sink.getIngestedEntities().size());
            assertEquals(source.numEntities(), result.getNumSucceeded());
            assertEquals(0, result.getNumFailed());
        } finally {
            ServiceFactory.setSourceServices(null);
            ServiceFactory.setSinkServices(null);
        }
    }

    @Test
    public void testSlurp() throws Exception {
        MockSourceService source = new MockSourceService();
        MockSinkService sink = new MockSinkService();

        SlurpResult result = instance.slurp(source, sink);
        assertEquals(source.numEntities(), sink.getIngestedEntities().size());
        assertEquals(source.numEntities(), result.getNumSucceeded());
        assertEquals(0, result.getNumFailed());
    }

    @Test
    public void testSlurpWithSourceNumItemsMethodThrowingIOException() {
        MockErroringSourceService1 source = new MockErroringSourceService1();
        MockSinkService sink = new MockSinkService();

        instance.slurp(source, sink);
    }

    @Test
    public void testSlurpWithSourceItemsMethodThrowingIOException() {
        MockErroringSourceService2 source = new MockErroringSourceService2();
        MockSinkService sink = new MockSinkService();

        instance.slurp(source, sink);
    }

}
