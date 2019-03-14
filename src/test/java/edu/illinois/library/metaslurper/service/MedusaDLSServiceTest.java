package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class MedusaDLSServiceTest {

    private MedusaDLSService instance;

    @Before
    public void setUp() {
        instance = new MedusaDLSService();
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test
    public void testNumEntities() throws Exception {
        assertTrue(instance.numEntities() > 100000);
    }

    @Test
    public void testNumEntitiesIncremental() {
        // This isn't really testable because a full DLS reindex will break any
        // implementation.
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();

        Entity entity = it.next();
        assertTrue(entity.getSinkID().startsWith(MedusaDLSService.ENTITY_ID_PREFIX));
    }

    @Test
    public void testEntitiesIncremental() throws Exception {
        instance.setLastModified(Instant.ofEpochSecond(1533913634));
        ConcurrentIterator<? extends Entity> it = instance.entities();

        Entity entity = it.next();
        assertTrue(entity.getSinkID().startsWith(MedusaDLSService.ENTITY_ID_PREFIX));
    }

}
