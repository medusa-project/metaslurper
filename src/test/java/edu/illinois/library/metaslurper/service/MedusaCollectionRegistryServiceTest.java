package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MedusaCollectionRegistryServiceTest {

    private MedusaCollectionRegistryService instance;

    @Before
    public void setUp() {
        instance = new MedusaCollectionRegistryService();
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test
    public void testNumEntities() throws Exception {
        assertTrue(instance.numEntities() > 1000);
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();

        Entity entity = it.next();
        assertTrue(entity.getSinkID().startsWith(
                MedusaCollectionRegistryService.ENTITY_ID_PREFIX));
    }

}
