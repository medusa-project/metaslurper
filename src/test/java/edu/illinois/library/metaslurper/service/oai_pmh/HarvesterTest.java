package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;

import java.time.Instant;

import static org.junit.Assert.*;

public class HarvesterTest {

    private static class MockElementTransformer implements ElementTransformer {

        @Override
        public Element transform(Node pmhNode) {
            return new Element("?" + pmhNode.getNodeName(),
                    "?" + pmhNode.getTextContent());
        }

    }

    private Harvester instance;

    @Before
    public void setUp() {
        instance = new Harvester();
        // N.B.: This endpoint should return a resumptionToken in an initial
        // ListRecords and ListSets response.
        instance.setEndpointURI("https://www.ideals.illinois.edu/dspace-oai/request");
    }

    @After
    public void tearDown() {
        instance.close();
    }

    @Test
    public void testNumRecords() throws Exception {
        assertTrue(instance.numRecords() > 1000);
    }

    @Test
    public void testNumRecordsWithDateRange() throws Exception {
        // 2017-03-05T00:00:00Z
        instance.setFrom(Instant.ofEpochSecond(1488672000));
        // 2017-05-05T00:00:00Z
        instance.setUntil(Instant.ofEpochSecond(1493942400));

        assertEquals(108, instance.numRecords());
    }

    @Test
    public void testNumSets() throws Exception {
        assertTrue(instance.numSets() > 120);
    }

    @Test
    public void testRecords() throws Exception {
        ConcurrentIterator<PMHRecord> it = instance.records();

        final int windowSize = 100;
        for (int i = 0; i < windowSize * 1.1; i++) {
            PMHRecord record = it.next();
            assertNotNull(record.getIdentifier());
            assertNotNull(record.getDatestamp());
            assertNotNull(record.getSetSpec());
            assertFalse(record.getElements().isEmpty());
        }
    }

    @Test
    public void testRecordsWithDateRange() throws Exception {
        // 2017-03-05T00:00:00Z
        instance.setFrom(Instant.ofEpochSecond(1488672000));
        // 2017-03-20T00:00:00Z
        instance.setUntil(Instant.ofEpochSecond(1489536000));

        ConcurrentIterator<PMHRecord> it = instance.records();

        int count = 0;
        while (true) {
            try {
                it.next();
                count++;
            } catch (EndOfIterationException e) {
                break;
            }
        }

        assertEquals(10, count);
    }

    @Test
    public void testRecordsWithElementTransformer() throws Exception {
        ConcurrentIterator<PMHRecord> it =
                instance.records(new MockElementTransformer());
        PMHRecord record = it.next();
        Element e = record.getElements().iterator().next();
        assertTrue(e.getName().startsWith("?"));
        assertTrue(e.getValue().startsWith("?"));
    }

    @Test
    public void testSets() throws Exception {
        ConcurrentIterator<PMHSet> it = instance.sets();

        final int windowSize = 100;
        for (int i = 0; i < windowSize * 1.1; i++) {
            PMHSet set = it.next();
            assertNotNull(set.getName());
            assertNotNull(set.getSpec());
            // TODO: most sets in IDEALS don't have any elements
            //assertFalse(set.getElements().isEmpty());
        }
    }

    @Ignore // TODO: most sets in IDEALS don't have any elements
    @Test
    public void testSetsWithElementTransformer() throws Exception {
        ConcurrentIterator<PMHSet> it =
                instance.sets(new MockElementTransformer());
        PMHSet set = it.next();
        Element e = set.getElements().iterator().next();
        assertTrue(e.getName().startsWith("?"));
        assertTrue(e.getValue().startsWith("?"));
    }

}
