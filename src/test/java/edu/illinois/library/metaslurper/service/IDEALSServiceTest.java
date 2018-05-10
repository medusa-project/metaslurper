package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.ConfigurationFactory;
import edu.illinois.library.metaslurper.entity.Entity;
import org.apache.commons.configuration2.Configuration;
import org.junit.Before;
import org.junit.Test;

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
        ConcurrentIterator<Entity> it = instance.entities();

        Entity entity = it.next();

        Configuration config = ConfigurationFactory.getConfiguration();
        String expectedPrefix = config.getString("service.source.ideals.key");

        assertTrue(entity.getSinkID().matches(expectedPrefix + "-[a-f0-9-]+"));
    }

}