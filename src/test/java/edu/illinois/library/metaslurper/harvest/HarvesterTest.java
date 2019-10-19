package edu.illinois.library.metaslurper.harvest;

import edu.illinois.library.metaslurper.service.MockAbortingSourceService;
import edu.illinois.library.metaslurper.service.MockErroringSinkService;
import edu.illinois.library.metaslurper.service.MockErroringSourceService1;
import edu.illinois.library.metaslurper.service.MockErroringSourceService2;
import edu.illinois.library.metaslurper.service.MockNonCountingSourceService;
import edu.illinois.library.metaslurper.service.MockOvercountingSourceService;
import edu.illinois.library.metaslurper.service.MockSinkService;
import edu.illinois.library.metaslurper.service.MockSourceService;
import edu.illinois.library.metaslurper.service.MockUndercountingSourceService;
import edu.illinois.library.metaslurper.service.MockUnreliableSourceService;
import edu.illinois.library.metaslurper.service.SourceService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class HarvesterTest {

    private Harvester instance;

    @Before
    public void setUp() {
        instance = new Harvester();
    }

    @Test
    public void testHarvestWithNoFailures() throws Exception {
        Harvest harvest = new Harvest();
        try (MockSourceService source = new MockSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
            assertEquals(source.numEntities(), sink.getIngestedEntities().size());
            assertEquals(source.numEntities(), harvest.getNumSucceeded());
            assertEquals(0, harvest.getNumFailed());
            assertEquals(0, harvest.numMessages());
            assertEquals(Lifecycle.SUCCEEDED, harvest.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithSourceFailures() {
        Harvest harvest = new Harvest();
        try (MockSourceService source = new MockUnreliableSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
            assertEquals(4, sink.getIngestedEntities().size());
            assertEquals(4, harvest.getNumSucceeded());
            assertEquals(1, harvest.getNumFailed());
            assertEquals(1, harvest.numMessages());
            assertEquals(Lifecycle.SUCCEEDED, harvest.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithOverCountingSource() throws Exception {
        Harvest harvest = new Harvest();
        try (MockSourceService source = new MockOvercountingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
            assertEquals(source.numEntities() - 1, sink.getIngestedEntities().size());
            assertEquals(source.numEntities() - 1, harvest.getNumSucceeded());
            assertEquals(0, harvest.getNumFailed());
            assertEquals(0, harvest.numMessages());
            assertEquals(Lifecycle.SUCCEEDED, harvest.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithUnderCountingSource() throws Exception {
        Harvest harvest = new Harvest();
        try (MockSourceService source = new MockUndercountingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
            assertEquals(source.numEntities() + 1, sink.getIngestedEntities().size());
            assertEquals(source.numEntities() + 1, harvest.getNumSucceeded());
            assertEquals(0, harvest.getNumFailed());
            assertEquals(0, harvest.numMessages());
            assertEquals(Lifecycle.SUCCEEDED, harvest.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithAbortingHarvest() throws IOException {
        Harvest harvest = new Harvest();
        try (MockSourceService source = new MockAbortingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
            assertEquals(0, sink.getIngestedEntities().size());
            assertEquals(0, harvest.getNumSucceeded());
            assertEquals(source.numEntities() - 1, harvest.getNumFailed());
            assertEquals(1, harvest.numMessages());
            assertEquals(Lifecycle.ABORTED, harvest.getLifecycle());
        }
    }

    @Test
    public void testHarvestWithSourceNumItemsMethodThrowingIOException() {
        Harvest harvest = new Harvest();
        try (MockErroringSourceService1 source = new MockErroringSourceService1();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
        }
        assertEquals(Lifecycle.FAILED, harvest.getLifecycle());
    }

    @Test
    public void testHarvestWithSourceNumItemsMethodThrowingUnsupportedOperationException() {
        Harvest harvest = new Harvest();
        try (SourceService source = new MockNonCountingSourceService();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
        }
        assertEquals(2, harvest.getNumSucceeded());
        assertEquals(0, harvest.getNumFailed());
        assertEquals(0, harvest.numMessages());
        assertEquals(Lifecycle.SUCCEEDED, harvest.getLifecycle());
    }

    @Test
    public void testHarvestWithSourceItemsMethodThrowingIOException() {
        Harvest harvest = new Harvest();
        try (MockErroringSourceService2 source = new MockErroringSourceService2();
             MockSinkService sink = new MockSinkService()) {
            instance.harvest(source, sink, harvest);
        }
        assertEquals(Lifecycle.FAILED, harvest.getLifecycle());
    }

    @Test
    public void testHarvestWithSinkFailures() {
        Harvest harvest = new Harvest();
        try (MockSourceService source = new MockSourceService();
             MockSinkService sink = new MockErroringSinkService()) {
            instance.harvest(source, sink, harvest);
            assertEquals(0, sink.getIngestedEntities().size());
            assertEquals(0, harvest.getNumSucceeded());
            assertEquals(5, harvest.getNumFailed());
            assertEquals(5, harvest.numMessages());
            assertEquals(Lifecycle.SUCCEEDED, harvest.getLifecycle());
        }
    }

}
