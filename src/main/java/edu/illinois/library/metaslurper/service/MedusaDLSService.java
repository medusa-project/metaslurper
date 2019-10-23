package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.harvest.HTTPException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Dolski UIUC
 */
final class MedusaDLSService implements SourceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaDLSService.class);

    static final String ENTITY_ID_PREFIX = "dls-";

    private static final String NAME = "Illinois Digital Library";

    static final long REQUEST_TIMEOUT = 30;

    private static HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private int numEntities = -1, windowSize = -1;

    private Instant lastModified;

    static synchronized HttpClient getClient() {
        if (client == null) {
            client = new HttpClient(new SslContextFactory());
            client.setFollowRedirects(true);

            AuthenticationStore auth = client.getAuthenticationStore();
            auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                    URI.create(getEndpointURI()),
                    getUsername(),
                    getSecret()));

            try {
                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return client;
    }

    /**
     * @return Base URI of the service.
     */
    private static String getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_DLS_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    /**
     * Contains a paginated list of all available entities: items, collections,
     * and agents.
     */
    private static String getHarvestURI() {
        return getEndpointURI() + "/harvest";
    }

    private static String getUsername() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_DLS_USERNAME");
    }

    private static String getSecret() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_DLS_SECRET");
    }

    static String getServiceKey() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_DLS_KEY");
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

    @Override
    public String getKey() {
        return getServiceKey();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int numEntities() throws HTTPException {
        if (numEntities < 0) {
            String uri = getHarvestURI();

            if (lastModified != null) {
                uri += "?last_modified_after=" + lastModified.getEpochSecond();
            }
            try {
                ContentResponse response = getClient()
                        .newRequest(uri)
                        .header("Accept", "application/json")
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                String body = response.getContentAsString();
                try {
                    JSONObject jobj = new JSONObject(body);
                    numEntities = jobj.getInt("numResults");
                    windowSize = jobj.getInt("windowSize");
                } catch (JSONException e) {
                    throw new HTTPException(
                            "GET",
                            uri,
                            response.getStatus(),
                            null,
                            response.getContentAsString(),
                            e);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException e) {
                throw new HTTPException("GET", uri, e);
            }
        }
        return numEntities;
    }

    /**
     * Provides an iterator of all entities in the service. Results pages and
     * entity representations are fetched on-demand during iteration.
     */
    @Override
    public ConcurrentIterator<? extends Entity> entities() {
        // Queue of entity URIs.
        final Queue<String> batch = new ConcurrentLinkedQueue<>();
        final AtomicInteger batchIndex = new AtomicInteger();

        // Return an iterator that consumes the queue.
        return new ConcurrentIterator<>() {
            @Override
            public Entity next() throws Exception {
                // If the queue is empty, fetch the next batch.
                synchronized (this) {
                    if (batch.peek() == null) {
                        fetchBatch(batch, batchIndex.getAndIncrement());
                    }
                }

                try {
                    return fetchEntity(batch.remove());
                } catch (NoSuchElementException e) {
                    throw new EndOfIterationException();
                }
            }
        };
    }

    /**
     * @param batch      Queue of entity URIs.
     * @param batchIndex Zero-based batch index.
     */
    private void fetchBatch(final Queue<String> batch,
                            final int batchIndex) throws IOException {
        if (isClosed.get()) {
            LOGGER.debug("fetchBatch(): stopping");
            return;
        }

        final int numResults = numEntities();
        final int numBatches = (int) Math.ceil(numResults / (double) windowSize);
        final int offset = batchIndex * windowSize;

        String uri = String.format("%s?start=%d", getHarvestURI(), offset);
        if (lastModified != null) {
            uri += "&last_modified_after=" + lastModified.getEpochSecond();
        }
        LOGGER.debug("Fetching batch {} of {}: {}",
                batchIndex + 1, numBatches, uri);

        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            String body = response.getContentAsString();
            if (response.getStatus() == 200) {
                JSONObject jobj = new JSONObject(body);
                JSONArray jarr = jobj.getJSONArray("results");
                for (int i = 0; i < jarr.length(); i++) {
                    batch.add(jarr.getJSONObject(i).getString("uri"));
                }
            } else {
                throw new HTTPException(
                        "GET",
                        uri,
                        response.getStatus(),
                        null,
                        response.getContentAsString());
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new HTTPException("GET", uri, e);
        }
        LOGGER.debug("Fetched {} results", batch.size());
    }

    private Entity fetchEntity(String uri) throws IOException {
        LOGGER.debug("Fetching entity: {}", uri);
        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            switch (response.getStatus()) {
                case HttpStatus.OK_200:
                    String body = response.getContentAsString();
                    JSONObject jobj = new JSONObject(body);
                    String variant = jobj.getString("class");
                    switch (variant) {
                        case "Agent":
                            return new MedusaDLSAgent(jobj, uri);
                        case "Collection":
                            return new MedusaDLSCollection(jobj);
                        case "Item":
                            return new MedusaDLSItem(jobj);
                        default:
                            throw new IllegalArgumentException(
                                    "Unrecognized variant: " + variant);
                    }
                default:
                    throw new HTTPException(
                            "GET",
                            uri,
                            response.getStatus(),
                            null,
                            response.getContentAsString());
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new HTTPException("GET", uri, e);
        }
    }

    @Override
    public void setLastModified(Instant lastModified)
            throws UnsupportedOperationException {
        this.lastModified = lastModified;
        this.numEntities = -1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}