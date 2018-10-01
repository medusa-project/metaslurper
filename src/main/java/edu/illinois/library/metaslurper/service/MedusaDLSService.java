package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Entity;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    /**
     * N.B.: 100 is the minimum and maximum the DLS allows.
     */
    private static final int BATCH_SIZE = 100;

    static final String ENTITY_ID_PREFIX = "dls-";

    private static final String NAME = "Illinois Digital Library";

    private static final long REQUEST_TIMEOUT = 30;

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private int numEntities = -1;

    private Instant lastModified;

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
    private static String getSearchURI() {
        return getEndpointURI() + "/search";
    }

    static String getKeyFromConfiguration() {
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
        return getKeyFromConfiguration();
    }

    @Override
    public String getName() {
        return NAME;
    }

    private synchronized HttpClient getClient() {
        if (client == null) {
            client = new HttpClient(new SslContextFactory());
            client.setFollowRedirects(true);
            try {
                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return client;
    }

    @Override
    public int numEntities() throws IOException {
        if (numEntities < 0) {
            String uri = getSearchURI();

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
                JSONObject jobj = new JSONObject(body);
                numEntities = jobj.getInt("numResults");
            } catch (ExecutionException | InterruptedException |
                    TimeoutException e) {
                throw new IOException(e);
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
        final int numBatches = (int) Math.ceil(numResults / (double) BATCH_SIZE);
        final int offset = batchIndex * BATCH_SIZE;

        String uri = String.format("%s?start=%d&limit=%d",
                getSearchURI(), offset, BATCH_SIZE);
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
            if (response.getStatus() == 200) {
                String body = response.getContentAsString();
                JSONObject jobj = new JSONObject(body);
                JSONArray jarr = jobj.getJSONArray("results");
                for (int i = 0; i < jarr.length(); i++) {
                    batch.add(jarr.getJSONObject(i).getString("uri"));
                }
            } else {
                throw new IOException("Got HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
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
            if ("application/json".equals(response.getMediaType())) {
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
                        body = response.getContentAsString();
                        jobj = new JSONObject(body);
                        String message = jobj.getString("error");

                        throw new IOException("Got HTTP " + response.getStatus() +
                                " for " + uri + ": " + message);
                }
            } else {
                throw new IOException("Unsupported response Content-Type: " +
                        response.getMediaType());
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
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
