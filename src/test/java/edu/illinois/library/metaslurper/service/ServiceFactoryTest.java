package edu.illinois.library.metaslurper.service;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class ServiceFactoryTest {

    @Test
    public void testAllSinkServices() {
        Set<SinkService> actual = ServiceFactory.allSinkServices();
        assertEquals(1, actual.size());
    }

    @Test
    public void testAllSourceServices() {
        Set<SourceService> actual = ServiceFactory.allSourceServices();
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetSinkServiceWithBogusName() {
        assertNull(ServiceFactory.getSinkService("bogus"));
    }

    @Test
    public void testGetSinkServiceWithValidName() {
        assertTrue(ServiceFactory.getSinkService(new MetaslurpService().getName())
                instanceof MetaslurpService);
    }

    @Test
    public void testGetSourceServiceWithBogusName() {
        assertNull(ServiceFactory.getSourceService("bogus"));
    }

    @Test
    public void testGetSourceServiceWithValidName() {
        assertTrue(ServiceFactory.getSourceService(new MedusaDLSService().getName())
                instanceof MedusaDLSService);
    }

}
