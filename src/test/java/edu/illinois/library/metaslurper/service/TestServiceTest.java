package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class TestServiceTest {

    private TestService instance;

    @Before
    public void setUp() {
        instance = new TestService();
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test
    public void testNumEntities() {
        assertEquals(2, instance.numEntities());
    }

    @Test
    public void testNumEntitiesIncremental() {
        instance.setLastModified(Instant.ofEpochSecond(1533913634));
        assertEquals(1, instance.numEntities());
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();

        Entity entity = it.next();
        assertNotNull(entity);
    }

    @Test
    public void testEntitiesIncremental() throws Exception {
        instance.setLastModified(Instant.ofEpochSecond(1533913634));
        ConcurrentIterator<? extends Entity> it = instance.entities();

        Entity entity = it.next();
        assertNotNull(entity);
    }

}
