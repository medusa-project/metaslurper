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

public class MedusaDLSCollectionTest {

    private static final String COLLECTION_URI =
            "https://digital.library.illinois.edu/harvest/collections/81180450-e3fb-012f-c5b6-0019b9e633c5-2.json";

    private static HttpClient client;

    private MedusaDLSCollection instance;

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
                .uri(URI.create(COLLECTION_URI))
                .build();
        HttpResponse<String> response = getClient().send(request,
                HttpResponse.BodyHandlers.ofString());

        String json    = response.body();
        JSONObject obj = new JSONObject(json);
        instance       = new MedusaDLSCollection(obj);
    }

    @Test
    public void testGetAccessImages() {
        Set<Image> expected = Set.of(
                new Image("s3://medusa-main/67/2412/access/11100019t.jp2",
                        Image.Crop.FULL, 0, true));
        Set<Image> actual = instance.getAccessImages();
        assertEquals(expected, actual);
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