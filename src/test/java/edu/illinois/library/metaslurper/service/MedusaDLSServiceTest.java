package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MedusaDLSServiceTest {

    private MedusaDLSService instance;

    @Before
    public void setUp() {
        instance = new MedusaDLSService();
    }

    @Test
    public void testNumEntities() throws Exception {
        assertTrue(instance.numEntities() > 100000);
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<Entity> it = instance.entities();

        Entity entity = it.next();
        assertTrue(entity.getSinkID().matches(MedusaDLSService.ENTITY_ID_PREFIX + "[a-f0-9-]+"));
    }

}
