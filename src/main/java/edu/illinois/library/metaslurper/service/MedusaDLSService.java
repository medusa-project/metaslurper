package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Entity;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Dolski UIUC
 */
final class MedusaDLSService implements SourceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaDLSService.class);

    static final String ENTITY_ID_PREFIX = "dls-";

    /**
     * Used within the application.
     */
    private static final String PRIVATE_NAME = "Illinois Digital Library";

    /**
     * Presented to public users.
     */
    static final String PUBLIC_NAME = "Digital Special Collections";

    static final long REQUEST_TIMEOUT = 30;

    private static OkHttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private int numEntities = -1, windowSize = -1;

    private Instant lastModified;

    static synchronized OkHttpClient getClient() {
        if (client == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .authenticator((route, response) -> {
                        String credential = Credentials.basic(getUsername(), getSecret());
                        return response.request().newBuilder()
                                .header("Authorization", credential).build();
                    })
                    .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS);
            client = builder.build();
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
            // If OkHttp isn't shut down manually, it will keep the app running
            // for a time after a harvest instead of immediately exiting.
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    @Override
    public String getKey() {
        return getServiceKey();
    }

    @Override
    public String getName() {
        return PRIVATE_NAME;
    }

    @Override
    public int numEntities() throws IOException {
        if (numEntities < 0) {
            String uri = getHarvestURI();
            if (lastModified != null) {
                uri += "?last_modified_after=" + lastModified.getEpochSecond();
            }
            Request.Builder builder = new Request.Builder()
                    .method("GET", null)
                    .header("Accept", "application/json")
                    .url(uri);
            Request request = builder.build();
            Response response = getClient().newCall(request).execute();
            try (ResponseBody body = response.body()) {
                final String bodyStr = body.string();
                try {
                    JSONObject jobj = new JSONObject(bodyStr);
                    numEntities     = jobj.getInt("numResults");
                    windowSize      = jobj.getInt("windowSize");
                } catch (JSONException e) {
                    throw new HTTPException(
                            "GET", uri, response.code(), null, bodyStr, e);
                }
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

        Request.Builder builder = new Request.Builder()
                .method("GET", null)
                .header("Accept", "application/json")
                .url(uri);
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();
        try (ResponseBody body = response.body()) {
            String bodyStr = body.string();
            if (response.code() == 200) {
                JSONObject jobj = new JSONObject(bodyStr);
                JSONArray jarr = jobj.getJSONArray("results");
                for (int i = 0; i < jarr.length(); i++) {
                    batch.add(jarr.getJSONObject(i).getString("uri"));
                }
            } else {
                throw new HTTPException(
                        "GET", uri, response.code(), null, bodyStr);
            }
        }
        LOGGER.debug("Fetched {} results", batch.size());
    }

    private Entity fetchEntity(String uri) throws IOException {
        LOGGER.debug("Fetching entity: {}", uri);

        Request.Builder builder = new Request.Builder()
                .method("GET", null)
                .header("Accept", "application/json")
                .url(uri);
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();
        try (ResponseBody body = response.body()) {
            final String bodyStr = body.string();
            switch (response.code()) {
                case 200:
                    JSONObject jobj = new JSONObject(bodyStr);
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
                            "GET", uri, response.code(), null, bodyStr);
            }
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