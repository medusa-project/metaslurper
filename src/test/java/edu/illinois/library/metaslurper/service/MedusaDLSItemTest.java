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
            "https://digital.library.illinois.edu/items/7f3c5580-9975-0134-2096-0050569601ca-8.json";

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
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/full/!128,128/0/default.jpg", 128, Image.Crop.FULL),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/square/!128,128/0/default.jpg", 128, Image.Crop.SQUARE),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/full/!256,256/0/default.jpg", 256, Image.Crop.FULL),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/square/!256,256/0/default.jpg", 256, Image.Crop.SQUARE),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/full/!512,512/0/default.jpg", 512, Image.Crop.FULL),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/square/!512,512/0/default.jpg", 512, Image.Crop.SQUARE),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/full/!1024,1024/0/default.jpg", 1024, Image.Crop.FULL),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/square/!1024,1024/0/default.jpg", 1024, Image.Crop.SQUARE),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/full/!2048,2048/0/default.jpg", 2048, Image.Crop.FULL),
                new Image("https://iiif.library.illinois.edu/dls/iiif/2/0291bca0-9978-0134-2096-0050569601ca-d/square/!2048,2048/0/default.jpg", 2048, Image.Crop.SQUARE)
        );
        assertEquals(expected, instance.getAccessImages());
    }

    @Test
    public void testGetElements() {
        assertEquals(20, instance.getElements().size());
    }

    @Test
    public void testGetMediaType() {
        assertNull(instance.getMediaType());
    }

    @Test
    public void testGetSinkID() {
        assertEquals("dls-7f3c5580-9975-0134-2096-0050569601ca-8",
                instance.getSinkID());
    }

    @Test
    public void testGetSourceID() {
        assertEquals("7f3c5580-9975-0134-2096-0050569601ca-8",
                instance.getSourceID());
    }

    @Test
    public void testGetSourceURI() {
        assertEquals("https://digital.library.illinois.edu/items/7f3c5580-9975-0134-2096-0050569601ca-8",
                instance.getSourceURI());
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.ITEM, instance.getVariant());
    }

}
