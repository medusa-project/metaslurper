package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class MedusaDLSServiceTest {

    private MedusaDLSService instance;

    @Before
    public void setUp() {
        instance = new MedusaDLSService();
    }

    @Test
    @Ignore // TODO: this takes too long to test
    public void testNumItems() throws Exception {
        assertTrue(instance.numItems() > 100000);
    }

    @Test
    public void testItems() throws Exception {
        ConcurrentIterator<Item> it = instance.items();

        Item item = (Item) it.next();
        assertTrue(item.getID().matches(MedusaDLSService.ITEM_ID_PREFIX + "[a-f0-9-]+"));
    }

}
