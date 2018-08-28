package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class MedusaBookTrackerServiceTest {

    private MedusaBookTrackerService instance;

    @Before
    public void setUp() {
        instance = new MedusaBookTrackerService();
    }

    @Test
    public void testNumEntities() throws Exception {
        assertTrue(instance.numEntities() > 50000);
    }

    @Test
    public void testNumEntitiesIncremental() throws Exception {
        int totalCount = instance.numEntities();

        instance.setLastModified(Instant.ofEpochSecond(1533913634));
        assertTrue(instance.numEntities() < totalCount);
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();

        for (int i = 0; i < 102; i++) {
            Entity entity = it.next();
            assertFalse(entity.getSinkID().isEmpty());
        }
    }

    @Test
    public void testEntitiesIncremental() throws Exception {
        instance.setLastModified(Instant.ofEpochSecond(1533913634));

        ConcurrentIterator<? extends Entity> it = instance.entities();
        assertNotNull(it.next());
    }

}
