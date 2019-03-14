package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class MedusaCollectionTest {

    private static final String COLLECTION_URI =
            "https://medusa.library.illinois.edu/collections/1410.json";

    private MedusaCollection instance;

    @Before
    public void setUp() throws Exception {
        instance = MedusaDLSService.fetchMedusaCollection(COLLECTION_URI);
    }

    @Test
    public void testGetAccessImages() {
        final String endpoint = "https://images.digital.library.illinois.edu/iiif/2";
        final String uuid = "b20fb9c0-530e-0136-4f2e-0050569601ca-f";
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
        assertTrue(instance.getElements().size() > 10);
        assertTrue(instance.getElements().size() < 20);
    }

    @Test
    public void testGetServiceKey() {
        assertEquals("mc", instance.getServiceKey());
    }

    @Test
    public void testGetSourceID() {
        assertEquals("d52b8bc0-1c3d-0137-6b79-02d0d7bfd6e4-e",
                instance.getSourceID());
    }

    @Test
    public void testGetSourceURI() {
        assertEquals("https://digital.library.illinois.edu/collections/d52b8bc0-1c3d-0137-6b79-02d0d7bfd6e4-e",
                instance.getSourceURI());
    }

    @Test
    public void testGetSinkID() {
        assertEquals("mc-d52b8bc0-1c3d-0137-6b79-02d0d7bfd6e4-e",
                instance.getSinkID());
    }

    @Test
    public void testGetParentSinkIDWhenParentIsAvailable() throws Exception {
        String uri = "https://medusa.library.illinois.edu/collections/993.json";
        instance = MedusaDLSService.fetchMedusaCollection(uri);

        assertEquals("mc-d52b8bc0-1c3d-0137-6b79-02d0d7bfd6e4-e",
                instance.getParentSinkID());
    }

    @Test
    public void testGetParentSinkIDWhenParentIsNotAvailable() {
        assertNull(instance.getParentSinkID());
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.COLLECTION, instance.getVariant());
    }

}
