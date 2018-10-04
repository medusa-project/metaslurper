package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.service.oai_pmh.Harvester;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHRecord;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IDEALSRecordTest {

    private IDEALSRecord instance;

    @Before
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        String endpointURI = config.getString("SERVICE_SOURCE_IDEALS_ENDPOINT");
        Harvester harvester = new Harvester();
        harvester.setEndpointURI(endpointURI);
        harvester.setMetadataPrefix("dim");

        ConcurrentIterator<PMHRecord> it =
                harvester.records(new DIMElementTransformer());
        instance = new IDEALSRecord(it.next());
    }

    @Test
    public void testGetElements() {
        assertEquals(18, instance.getElements().size());
    }

    @Test
    public void testGetMediaType() {
        assertTrue(instance.getMediaType().matches("\\w+\\/[\\w-]+"));
    }

    @Test
    public void testGetSinkID() {
        assertTrue(instance.getSinkID().startsWith("ideals-http_"));
    }

    @Test
    public void testGetSourceID() {
        assertTrue(instance.getSourceID().contains("://hdl.handle.net/"));
    }

    @Test
    public void testGetSourceURI() {
        assertTrue(instance.getSourceURI().contains("://hdl.handle.net/"));
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.PAPER, instance.getVariant());
    }

}
