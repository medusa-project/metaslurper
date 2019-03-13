package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Entity;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class MedusaCollectionRegistryService implements SourceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaCollectionRegistryService.class);

    static final String ENTITY_ID_PREFIX = "mc";

    private static final String NAME = "Medusa Collection Registry";

    private static final long REQUEST_TIMEOUT = 30;

    private static HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private final List<String> collectionURIs = new ArrayList<>(2000);

    static URI getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_MEDUSA_ENDPOINT");
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static String getKeyFromConfiguration() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_MEDUSA_KEY");
    }

    /**
     * @return IIIF Image API endpoint URI. IIIF Image API parameters will be
     *         appended to this string.
     */
    static String getIIIFEndpointURI() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_MEDUSA_IIIF_ENDPOINT");
    }

    /**
     * @return HTTP Basic username.
     */
    static String getUsername() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_MEDUSA_USERNAME");
    }

    /**
     * @return HTTP Basic secret.
     */
    static String getSecret() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_MEDUSA_SECRET");
    }

    private static synchronized HttpClient getClient() {
        if (client == null) {
            client = new HttpClient(new SslContextFactory());
            client.setFollowRedirects(true);
            try {
                if (getUsername() != null && getSecret() != null) {
                    AuthenticationStore auth = client.getAuthenticationStore();
                    auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                            getEndpointURI(), getUsername(), getSecret()));
                }
                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return client;
    }

    /**
     * Fetches a collection from {@literal /collections/:id}.
     */
    static MedusaCollection fetchCollection(String uri) throws IOException {
        LOGGER.debug("Fetching collection: {}", uri);

        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            if (response.getStatus() == 200) {
                String body = response.getContentAsString();
                JSONObject jcol = new JSONObject(body);
                return new MedusaCollection(jcol);
            } else {
                throw new IOException("Got HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        isClosed.set(true);

        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                LOGGER.error("close(): " + e.getMessage());
            }
        }
    }

    private void checkClosed() {
        if (isClosed.get()) {
            throw new IllegalStateException("Instance is closed");
        }
    }

    @Override
    public ConcurrentIterator<? extends Entity> entities() {
        checkClosed();

        final AtomicInteger index = new AtomicInteger();

        return new ConcurrentIterator<>() {
            @Override
            public synchronized Entity next() throws Exception {
                synchronized (MedusaCollectionRegistryService.class) {
                    if (collectionURIs.isEmpty()) {
                        fetchCollectionsList();
                    }
                }
                try {
                    // Keep checking collections until a published one is found.
                    while (index.get() < collectionURIs.size()) {
                        String uri = collectionURIs.get(index.getAndIncrement());
                        MedusaCollection col = fetchCollection(uri);
                        if (col.isPublished()) {
                            return col;
                        }
                    }
                    throw new EndOfIterationException();
                } catch (IndexOutOfBoundsException e) {
                    throw new EndOfIterationException();
                }
            }
        };
    }

    /**
     * Fetches the collections list from {@literal /collections}.
     */
    private void fetchCollectionsList() throws IOException {
        final String uri = String.format("%s/collections.json",
                getEndpointURI());
        LOGGER.debug("Fetching collections: {}", uri);

        collectionURIs.clear();

        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            if (response.getStatus() == 200) {
                String body = response.getContentAsString();

                JSONArray results = new JSONArray(body);
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jcol = results.getJSONObject(i);
                    collectionURIs.add(
                            getEndpointURI() + jcol.getString("path") + ".json");
                }
                LOGGER.debug("Fetched {} collections", collectionURIs.size());
            } else {
                throw new IOException("Got HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getKey() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_MEDUSA_KEY");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int numEntities() throws IOException {
        checkClosed();
        synchronized (MedusaCollectionRegistryService.class) {
            if (collectionURIs.isEmpty()) {
                fetchCollectionsList();
            }
            return collectionURIs.size();
        }
    }

    @Override
    public void setLastModified(Instant lastModified) {
        // This service doesn't support incremental harvesting because it
        // contains only a few thousand harvestable entities at most.
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
