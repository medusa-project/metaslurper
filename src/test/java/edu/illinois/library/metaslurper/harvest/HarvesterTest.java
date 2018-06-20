package edu.illinois.library.metaslurper.harvest;

import edu.illinois.library.metaslurper.service.MockAbortingSourceService;
import edu.illinois.library.metaslurper.service.MockErroringSourceService1;
import edu.illinois.library.metaslurper.service.MockErroringSourceService2;
import edu.illinois.library.metaslurper.service.MockOvercountingSourceService;
import edu.illinois.library.metaslurper.service.MockSinkService;
import edu.illinois.library.metaslurper.service.MockSourceService;
import edu.illinois.library.metaslurper.service.MockUndercountingSourceService;
import edu.illinois.library.metaslurper.service.MockUnreliableSourceService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HarvesterTest {

    private Harvester instance;

    @Before
    public void setUp() {
        instance = new Harvester();
    }

    @Test
    public void testHarvestWithNoFailures() throws Exception {
        Status status = new Status();
        try (MockSourceService source = new MockSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
            assertEquals(source.numEntities(), sink.getIngestedEntities().size());
            assertEquals(source.numEntities(), status.getNumSucceeded());
            assertEquals(0, status.getNumFailed());
            assertEquals(Lifecycle.SUCCEEDED, status.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithFailures() {
        Status status = new Status();
        try (MockSourceService source = new MockUnreliableSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
            assertEquals(1, sink.getIngestedEntities().size());
            assertEquals(1, status.getNumSucceeded());
            assertEquals(1, status.getNumFailed());
            assertEquals(Lifecycle.SUCCEEDED, status.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithOvercountingSource() throws Exception {
        Status status = new Status();
        try (MockSourceService source = new MockOvercountingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
            assertEquals(source.numEntities() - 1, sink.getIngestedEntities().size());
            assertEquals(source.numEntities() - 1, status.getNumSucceeded());
            assertEquals(1, status.getNumFailed());
            assertEquals(Lifecycle.SUCCEEDED, status.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithUndercountingSource() throws Exception {
        Status status = new Status();
        try (MockSourceService source = new MockUndercountingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
            assertEquals(source.numEntities() + 1, sink.getIngestedEntities().size());
            assertEquals(source.numEntities() + 1, status.getNumSucceeded());
            assertEquals(0, status.getNumFailed());
            assertEquals(Lifecycle.SUCCEEDED, status.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithAbortingHarvest() throws Exception {
        Status status = new Status();
        try (MockSourceService source = new MockAbortingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
            assertEquals(0, sink.getIngestedEntities().size());
            assertEquals(0, status.getNumSucceeded());
            assertEquals(source.numEntities(), status.getNumFailed());
            assertEquals(Lifecycle.ABORTED, status.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithSourceNumItemsMethodThrowingIOException() {
        Status status = new Status();
        try (MockErroringSourceService1 source = new MockErroringSourceService1();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
        }
        assertEquals(Lifecycle.FAILED, status.getLifecycle());
    }

    @Test
    public void testHarvestWithSourceItemsMethodThrowingIOException() {
        Status status = new Status();
        try (MockErroringSourceService2 source = new MockErroringSourceService2();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, status);
        }
        assertEquals(Lifecycle.FAILED, status.getLifecycle());
    }

}
