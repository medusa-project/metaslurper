package edu.illinois.library.metaslurper.service.oai_pmh;

import org.junit.Before;
import org.junit.Test;

import javax.xml.XMLConstants;

import static org.junit.Assert.*;

public class OAINamespaceContextTest {

    private OAINamespaceContext instance;

    @Before
    public void setUp() {
        instance = new OAINamespaceContext();
    }

    @Test(expected = NullPointerException.class)
    public void testGetNamespaceURIWithNullArgument() {
        instance.getNamespaceURI(null);
    }

    @Test
    public void testGetNamespaceURIWithDC() {
        assertEquals("http://purl.org/dc/elements/1.1/",
                instance.getNamespaceURI("dc"));
    }

    @Test
    public void testGetNamespaceURIWithOAI() {
        assertEquals("http://www.openarchives.org/OAI/2.0/",
                instance.getNamespaceURI("oai"));
    }

    @Test
    public void testGetNamespaceURIWithOAIDC() {
        assertEquals("http://www.openarchives.org/OAI/2.0/oai_dc/",
                instance.getNamespaceURI("oai_dc"));
    }

    @Test
    public void testGetNamespaceURIWithAnyOtherPrefix() {
        assertEquals(XMLConstants.NULL_NS_URI,
                instance.getNamespaceURI("whatever"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetPrefix() {
        instance.getPrefix("http://example.org");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetPrefixes() {
        instance.getPrefixes("http://example.org");
    }

}
