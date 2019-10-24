package edu.illinois.library.metaslurper.harvest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HarvestTest {

    private Harvest instance;

    @Before
    public void setUp() {
        instance = new Harvest();
        instance.setNumEntities(50);
    }

    @Test
    public void testAbort1() {
        instance.abort();
        assertEquals(Lifecycle.ABORTED, instance.getLifecycle());
        assertEquals(instance.getNumEntities(), instance.getNumFailed());
    }

    @Test
    public void testAbort2() {
        instance.incrementNumSucceeded();
        instance.incrementNumSucceeded();
        instance.incrementNumFailed();
        instance.abort();
        assertEquals(Lifecycle.ABORTED, instance.getLifecycle());
        assertEquals(instance.getNumEntities() - 2, instance.getNumFailed());
    }

    @Test
    public void testAddMessageRespectsLimit() {
        for (int i = 0; i < 120; i++) {
            instance.addMessage("Hello");
        }
        assertEquals(100, instance.numMessages());
    }

    @Test
    public void testEndPrematurely1() {
        instance.end();
        assertEquals(Lifecycle.SUCCEEDED, instance.getLifecycle());
        assertEquals(instance.getNumEntities(), instance.getNumFailed());
    }

    @Test
    public void testEndPrematurely2() {
        instance.incrementNumSucceeded();
        instance.incrementNumSucceeded();
        instance.incrementNumFailed();
        instance.end();
        assertEquals(Lifecycle.SUCCEEDED, instance.getLifecycle());
        assertEquals(instance.getNumEntities() - 2, instance.getNumFailed());
    }

}
