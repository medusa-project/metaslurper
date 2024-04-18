package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.Assert.*;

public class IDNCEntityTest {

    private static final String PAGE_URL =
            "https://idnc.library.illinois.edu/cgi-bin/illinois?a=d&d=CHP19370109.1.4&f=XML";

    private IDNCEntity instance;

    @Before
    public void setUp() throws Exception {
        URL url = new URL(PAGE_URL);
        try (InputStream is = url.openStream()) {
            instance = IDNCEntity.fromXML(is);
        }
    }

    @Test
    public void testFromXMLWithString() throws Exception {
        URL url = new URL(PAGE_URL);
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            String str = new String(bytes, StandardCharsets.UTF_8);
            instance = IDNCEntity.fromXML(str);
        }
    }

    @Test
    public void testFromXMLWithInputStream() {
        // this is tested in setUp()
    }

    @Test
    public void testGetAccessImageURIs() {
        Set<Image> expected = Set.of(
                new Image("https://idnc.library.illinois.edu/?a=is&oid=CHP19370109.1.4&type=rawimage",
                        Image.Crop.FULL, 0, true));
        Set<Image> actual = instance.getAccessImages();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetElements() {
        Set<Element> elements = instance.getElements();
        assertEquals(6, elements.size());
        assertEquals("Chicago Packer",
                elementValue("publicationTitle", elements));
        assertEquals("Chicago Packer, 9 January 1937 - Page 4",
                elementValue("title", elements));
        assertEquals("CHP19370109.1.3",
                elementValue("previousPageID", elements));
        assertEquals("Digitized Newspapers",
                elementValue("service", elements));
        assertEquals("9 January 1937",
                elementValue("date", elements));
        assertEquals("CHP19370109.1.5",
                elementValue("nextPageID", elements));
    }

    @Test
    public void testGetFullText() {
        assertTrue(instance.getFullText().startsWith("MEYER"));
    }

    @Test
    public void testGetSinkID() {
        assertEquals("idnc-CHP19370109_1_4", instance.getSinkID());
    }

    @Test
    public void testGetSourceID() {
        assertEquals("CHP19370109.1.4", instance.getSourceID());
    }

    @Test
    public void testGetSourceURI() {
        assertEquals("https://idnc.library.illinois.edu/?a=d&d=CHP19370109.1.4",
                instance.getSourceURI());
    }

    @Test
    public void testGetVariant() {
        assertEquals(Variant.NEWSPAPER_PAGE, instance.getVariant());
    }

    private String elementValue(String elementName, Set<Element> elements) {
        return elements.stream()
                .filter(e -> elementName.equals(e.getName()))
                .map(Element::getValue)
                .findFirst()
                .orElse(null);
    }

}