package edu.illinois.library.metaslurper.service.oai_pmh;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class HarvesterTest {

    private Harvester instance;

    @Before
    public void setUp() {
        instance = new Harvester();
        instance.setEndpointURI("https://digital.library.illinois.edu/oai-pmh");
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test
    public void testGetNumRecords() throws Exception {
        assertTrue(instance.getNumRecords() > 1000);
    }

    @Test
    public void testGetNumSets() throws Exception {
        assertTrue(instance.getNumSets() > 10);
    }

    @Test
    @Ignore
    public void testHarvest() {
        // TODO: write this
    }

}
