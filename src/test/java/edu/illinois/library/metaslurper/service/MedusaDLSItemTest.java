package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.Assert.*;

public class MedusaDLSItemTest {

    private static final String ITEM_URI =
            "https://digital.library.illinois.edu/harvest/items/e9edb400-c556-0134-2373-0050569601ca-c.json";

    private static HttpClient client;

    private MedusaDLSItem instance;

    private static HttpClient getClient() {
        if (client == null) {
            client = HttpClient.newBuilder()
                    .authenticator(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            String username = System.getenv("SERVICE_SOURCE_DLS_USERNAME");
                            String secret   = System.getenv("SERVICE_SOURCE_DLS_SECRET");
                            return new PasswordAuthentication(username, secret.toCharArray());
                        }
                    })
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        }
        return client;
    }

    @Before
    public void setUp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ITEM_URI))
                .build();
        HttpResponse<String> response = getClient().send(request,
                HttpResponse.BodyHandlers.ofString());

        String json    = response.body();
        JSONObject obj = new JSONObject(json);
        instance       = new MedusaDLSItem(obj);
    }

    @Test
    public void testGetAccessImages() {
        Set<Image> expected = Set.of(
                new Image("s3://medusa-main/1164/2754/2519/access/2014_12996_393_004.jp2",
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
