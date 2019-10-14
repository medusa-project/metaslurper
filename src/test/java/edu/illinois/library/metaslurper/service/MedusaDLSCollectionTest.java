package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class MedusaDLSCollectionTest {

    private static final String COLLECTION_URI =
            "https://digital.library.illinois.edu/collections/81180450-e3fb-012f-c5b6-0019b9e633c5-2.json";

    private MedusaDLSCollection instance;

    @Before
    public void setUp() throws Exception {
        URL url = new URL(COLLECTION_URI);
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            instance = new MedusaDLSCollection(obj);
        }
    }

    @Test
    public void testGetAccessImages() {
        assertFalse(instance.getAccessImages().isEmpty());
    }

    @Test
    public void testGetElements() {
        assertTrue(instance.getElements().size() > 5);
    }

    @Test
    public void testGetMediaType() {
        assertNull(instance.getMediaType());
    }

    @Test
    public void testGetSinkID() {
        assertEquals("dls-81180450-e3fb-012f-c5b6-0019b9e633c5-2",
                instance.getSinkID());
    }

    @Test
    public void testGetSourceID() {
        assertEquals("81180450-e3fb-012f-c5b6-0019b9e633c5-2",
                instance.getSourceID());
    }

    @Test
    public void testGetSourceURI() {
        assertEquals("https://digital.library.illinois.edu/collections/81180450-e3fb-012f-c5b6-0019b9e633c5-2",
                instance.getSourceURI());
    }

    @Test
    public void testGetContainerName() {
        assertEquals("University of Illinois Archives",
                instance.getContainerName());
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.COLLECTION, instance.getVariant());
    }

}