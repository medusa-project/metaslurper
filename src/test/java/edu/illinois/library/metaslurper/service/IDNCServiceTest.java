package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IDNCServiceTest {

    private IDNCService instance;

    @Before
    public void setUp() {
        instance = new IDNCService();
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNumEntities() {
        instance.numEntities();
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();

        Entity entity = it.next();
        assertTrue(entity.getSinkID().startsWith(IDNCService.ENTITY_ID_PREFIX));
    }

}