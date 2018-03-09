package edu.illinois.library.metaslurper.service;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class ServiceFactoryTest {

    @Test
    public void testAllServices() {
        Set<Service> actual = ServiceFactory.allServices();
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetServiceWithBogusName() {
        assertNull(ServiceFactory.getService("bogus"));
    }

    @Test
    public void testGetServiceWithValidName() {
        assertTrue(ServiceFactory.getService(new MedusaDLSService().getName())
                instanceof MedusaDLSService);
    }

}
