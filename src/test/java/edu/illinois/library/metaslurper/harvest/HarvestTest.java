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
        instance.getAndIncrementIndex();
        instance.getAndIncrementIndex();
        instance.getAndIncrementIndex();
        instance.abort();
        assertEquals(Lifecycle.ABORTED, instance.getLifecycle());
        assertEquals(instance.getNumEntities() - 3, instance.getNumFailed());
    }

    @Test
    public void testEndPrematurely1() {
        instance.endPrematurely();
        assertEquals(Lifecycle.SUCCEEDED, instance.getLifecycle());
        assertEquals(instance.getNumEntities(), instance.getNumFailed());
    }

    @Test
    public void testEndPrematurely2() {
        instance.getAndIncrementIndex();
        instance.getAndIncrementIndex();
        instance.getAndIncrementIndex();
        instance.endPrematurely();
        assertEquals(Lifecycle.SUCCEEDED, instance.getLifecycle());
        assertEquals(instance.getNumEntities() - 3, instance.getNumFailed());
    }

}
