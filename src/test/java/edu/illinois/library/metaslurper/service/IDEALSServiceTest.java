package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class IDEALSServiceTest {

    private IDEALSService instance;

    @Before
    public void setUp() {
        instance = new IDEALSService();
    }

    @Test
    public void testNumEntities() throws Exception {
        assertTrue(instance.numEntities() > 10000);
    }

    @Test
    public void testEntities() throws Exception {
        ConcurrentIterator<? extends Entity> it = instance.entities();
        ConcreteEntity entity = (ConcreteEntity) it.next();

        // Check the entity's sink ID
        Configuration config = Configuration.getInstance();
        String expectedPrefix = config.getString("SERVICE_SOURCE_IDEALS_KEY");
        assertTrue(entity.getSinkID().matches(expectedPrefix + "-[A-Za-z\\d_]+"));

        // Check its elements
        Set<Element> elements = entity.getElements();
        assertTrue(elements.size() > 0);
    }

}
