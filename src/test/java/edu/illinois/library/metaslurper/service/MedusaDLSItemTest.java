package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.Assert.*;

public class MedusaDLSItemTest {

    private static final String ITEM_URI =
            "https://digital.library.illinois.edu/items/e9edb400-c556-0134-2373-0050569601ca-c.json";

    private MedusaDLSItem instance;

    @Before
    public void setUp() throws Exception {
        URL url = new URL(ITEM_URI);
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            instance = new MedusaDLSItem(obj);
        }
    }

    @Test
    public void testGetAccessImages() {
        Set<Image> expected = Set.of(
                new Image("s3://medusa-main/1164/2754/2519/access/2014_12996_393_001.jp2",
                        Image.Crop.FULL, 0, true));
        Set<Image> actual = instance.getAccessImages();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetContainerSinkID() {
        assertEquals("dls-692ae4c0-c09b-0134-2371-0050569601ca-6",
                instance.getContainerSinkID());
    }

    @Test
    public void testGetElements() {
        assertTrue(instance.getElements().size() > 10);
    }

    @Test
    public void testGetMediaType() {
        assertNull(instance.getMediaType());
    }

    @Test
    public void testGetSinkID() {
        assertEquals("dls-e9edb400-c556-0134-2373-0050569601ca-c",
                instance.getSinkID());
    }

    @Test
    public void testGetSourceID() {
        assertEquals("e9edb400-c556-0134-2373-0050569601ca-c",
                instance.getSourceID());
    }

    @Test
    public void testGetSourceURI() {
        assertEquals("https://digital.library.illinois.edu/items/e9edb400-c556-0134-2373-0050569601ca-c",
                instance.getSourceURI());
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.ITEM, instance.getVariant());
    }

}
