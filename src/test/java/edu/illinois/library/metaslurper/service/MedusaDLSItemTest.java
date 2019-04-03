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
                        Image.Crop.FULL, 0, true),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!64,64/0/default.jpg",
                        Image.Crop.FULL, 64, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!128,128/0/default.jpg",
                        Image.Crop.FULL, 128, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!256,256/0/default.jpg",
                        Image.Crop.FULL, 256, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!512,512/0/default.jpg",
                        Image.Crop.FULL, 512, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!1024,1024/0/default.jpg",
                        Image.Crop.FULL, 1024, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!2048,2048/0/default.jpg",
                        Image.Crop.FULL, 2048, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/full/!4096,4096/0/default.jpg",
                        Image.Crop.FULL, 4096, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!64,64/0/default.jpg",
                        Image.Crop.SQUARE, 64, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!128,128/0/default.jpg",
                        Image.Crop.SQUARE, 128, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!256,256/0/default.jpg",
                        Image.Crop.SQUARE, 256, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!512,512/0/default.jpg",
                        Image.Crop.SQUARE, 512, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!1024,1024/0/default.jpg",
                        Image.Crop.SQUARE, 1024, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!2048,2048/0/default.jpg",
                        Image.Crop.SQUARE, 2048, false),
                new Image("https://images.digital.library.illinois.edu/iiif/2/32cda5f0-c55f-0134-2373-0050569601ca-5/square/!4096,4096/0/default.jpg",
                        Image.Crop.SQUARE, 4096, false));
        Set<Image> actual = instance.getAccessImages();
        assertEquals(expected, actual);
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
