package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
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
        final String endpoint = "https://images.digital.library.illinois.edu/iiif/2";
        final String uuid = "0291bca0-9978-0134-2096-0050569601ca-d";
        final Set<Image> expected = new HashSet<>();

        for (int i = (int) Math.pow(2, ConcreteEntity.MIN_ACCESS_IMAGE_POWER);
             i <= Math.pow(2, ConcreteEntity.MAX_ACCESS_IMAGE_POWER);
             i *= 2) {
            final int size = i;
            Arrays.stream(Image.Crop.values()).forEach((crop) -> {
                String uri = String.format("%s/%s/%s/!%d,%d/0/default.jpg",
                        endpoint, uuid, crop.toIIIFRegion(), size, size);
                expected.add(new Image(uri, size, crop));
            });
        }
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
