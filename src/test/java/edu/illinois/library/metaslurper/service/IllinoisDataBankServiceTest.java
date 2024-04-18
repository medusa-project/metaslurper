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
        assertTrue(instance.numEntities() > 700);
    }

    @Test
    public void testEntities() throws Exception {
        final ConcurrentIterator<? extends Entity> it = instance.entities();
        // Test only a few, rather than iterating through all 700+ of them.
        for (int i = 0; i < 10; i++) {
            try {
                Entity entity = it.next();
                assertFalse(entity.getSinkID().isBlank());
            } catch (HTTPException e) {
                // Some items are restricted; that's OK.
                if (e.getStatusCode().get() != 403) {
                    throw e;
                }
            } catch (EndOfIterationException e) {
                break;
            }
        }
    }

}
