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
        final int count = instance.numEntities();
        final ConcurrentIterator<? extends Entity> it = instance.entities();
        int i = 0;
        while (true) {
            try {
                Entity entity = it.next();
                assertFalse(entity.getSinkID().isEmpty());
            } catch (HTTPException e) {
                // Some items are restricted; that's OK.
                if (e.getStatusCode().get() != 403) {
                    throw e;
                }
            } catch (EndOfIterationException e) {
                break;
            } finally {
                i++;
            }
        }
        assertEquals(count, i);
    }

}
