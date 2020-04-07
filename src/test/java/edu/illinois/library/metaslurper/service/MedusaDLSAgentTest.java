package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.Assert.*;

public class MedusaDLSAgentTest {

    private static final String AGENT_URI =
            "https://digital.library.illinois.edu/agents/1.json";

    private MedusaDLSAgent instance;

    @Before
    public void setUp() throws Exception {
        URL url = new URL(AGENT_URI);
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            instance = new MedusaDLSAgent(obj, AGENT_URI);
        }
    }

    @Test
    public void testGetAccessImages() {
        assertTrue(instance.getAccessImages().isEmpty());
    }

    @Test
    public void testGetElements() {
        Set<Element> expected = Set.of(
                new Element("name", "Motley, Harriette"),
                new Element("service", "Digital Special Collections"));
        Set<Element> actual = instance.getElements();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetMediaType() {
        assertNull(instance.getMediaType());
    }

    @Test
    public void testGetSinkID() {
        assertEquals("dls-entity-1", instance.getSinkID());
    }

    @Test
    public void testGetSourceID() {
        assertEquals("1", instance.getSourceID());
    }

    @Test
    public void testGetSourceURI() {
        assertEquals("https://digital.library.illinois.edu/agents/1.json",
                instance.getSourceURI());
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.ENTITY, instance.getVariant());
    }

}
