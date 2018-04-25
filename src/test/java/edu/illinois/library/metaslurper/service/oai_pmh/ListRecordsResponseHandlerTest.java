package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.*;

public class ListRecordsResponseHandlerTest extends OAIPMHResponseHandlerTest {

    private static class MockListener implements Listener {

        private final Set<Set<Element>> records = new HashSet<>();

        @Override
        public void onRecord(Set<Element> elements) {
            records.add(elements);
        }

        @Override
        public void onSet(Set<Element> elements) {}

        @Override
        public void onError(OAIPMHException error) {}

        Set<Set<Element>> getRecords() {
            return records;
        }

    }

    private MockListener listener = new MockListener();

    @Before
    public void setUp() {
        instance = new ListRecordsResponseHandler();
        instance.setListener(listener);
    }

    @Override
    Path getErrorResponse() {
        return Paths.get("./src/test/resources/edu/illinois/library/metaslurper/service/oai_pmh/ListRecords-error.xml");
    }

    @Override
    Path getValidResponse() {
        return Paths.get("./src/test/resources/edu/illinois/library/metaslurper/service/oai_pmh/ListRecords.xml");
    }

    @Test
    public void testGetCompleteListSize() throws Exception {
        readValidResponse();
        int size = instance.getCompleteListSize();
        assertTrue(size > 10000);
        assertTrue(size < 100000);
    }

    @Test(expected = OAIPMHException.class)
    public void testGetCompleteListSizeWithError() throws Exception {
        readErrorResponse();
        instance.getCompleteListSize();
    }

    @Test
    public void testGetResumptionToken() throws Exception {
        readValidResponse();
        String token = instance.getResumptionToken();
        assertTrue(token.length() > 5);
    }

    @Test(expected = OAIPMHException.class)
    public void testGetResumptionTokenWithError() throws Exception {
        readErrorResponse();
        instance.getResumptionToken();
    }

    @Test
    public void testListener() throws Exception {
        readValidResponse();
        assertEquals(100, listener.getRecords().size());

        Iterator<Set<Element>> it = listener.getRecords().iterator();
        Set<Element> record = it.next();
        assertTrue(record.size() > 1);
    }

}
