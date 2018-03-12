package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MedusaDLSServiceTest {

    private MedusaDLSService instance;

    @Before
    public void setUp() {
        instance = new MedusaDLSService();
    }

    @Test
    @Ignore // TODO: this takes too long to test
    public void testNumItems() {
        assertTrue(instance.numItems() > 100000);
    }

    @Test
    public void testItems() {
        try (Stream<Item> stream = instance.items().limit(2)) {
            assertEquals(2, stream.count());
        }
        try (Stream<Item> stream = instance.items()) {
            Item item = stream.findFirst().get();
            assertTrue(item.getID().matches(MedusaDLSService.ITEM_ID_PREFIX + "[a-f0-9-]+"));
        }
    }

}
