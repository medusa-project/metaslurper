package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IllinoisDataBankServiceTest {

    private IllinoisDataBankService instance;

    @Before
    public void setUp() {
        instance = new IllinoisDataBankService();
    }

    @Test
    public void testNumEntities() throws Exception {
        assertTrue(instance.numEntities() > 50);
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();

        final int count = instance.numEntities();

        for (int i = 0; i < count; i++) {
            Entity entity = it.next();
            assertFalse(entity.getSinkID().isEmpty());
        }
    }

}
