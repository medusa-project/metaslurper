package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.service.oai_pmh.Harvester;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHSet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IDEALSSetTest {

    private IDEALSSet instance;

    @Before
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        String endpointURI = config.getString("SERVICE_SOURCE_IDEALS_ENDPOINT");
        Harvester harvester = new Harvester();
        harvester.setEndpointURI(endpointURI);
        harvester.setMetadataPrefix("dim");

        ConcurrentIterator<PMHSet> it = harvester.sets();
        instance = new IDEALSSet(it.next());
    }

    @Test
    public void testGetElements() {
        assertEquals(3, instance.getElements().size());
    }

    @Test
    public void testGetMediaType() {
        assertNull(instance.getMediaType());
    }

    @Test
    public void testGetSinkID() {
        assertTrue(instance.getSinkID().startsWith("ideals-com_"));
    }

    @Test
    public void testGetSourceID() {
        assertTrue(instance.getSourceID().matches("com_\\d+_\\d"));
    }

    @Test
    public void testGetSourceURI() {
        assertTrue(instance.getSourceURI().contains("://hdl.handle.net/"));
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.COLLECTION, instance.getVariant());
    }

}
