package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Entity;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Harvests items and agents from the DLS, and collections from the Medusa
 * Collection Registry.
 *
 * @author Alex Dolski UIUC
 */
final class MedusaDLSService implements SourceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaDLSService.class);

    static final String ENTITY_ID_PREFIX = "dls-";

    /**
     * N.B.: 100 is the minimum and maximum the DLS allows.
     */
    private static final int BATCH_SIZE = 100;

    private static final String NAME = "Illinois Digital Special Collections";

    private static final long REQUEST_TIMEOUT = 30;

    private static HttpClient dlsClient, medusaClient;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private int numEntities = -1;

    /**
     * Set of published Medusa collections.
     */
    private final List<MedusaCollection> collections = new ArrayList<>();

    private Instant lastModified;

    /**
     * @return Base URI of the DLS.
     */
    static String getDLSEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_DLS_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    /**
     * Contains a paginated list of all available entities: items, collections,
     * and agents.
     */
    private static String getDLSHarvestURI() {
        return getDLSEndpointURI() + "/harvest";
    }

    /**
     * @return Base URI of the service's IIIF Image API endpoint.
     */
    static String getIIIFEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_DLS_IIIF_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    /**
     * @return Base URI of the Medusa Collection Registry.
     */
    static String getMedusaEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_DLS_MEDUSA_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    /**
     * @return HTTP Basic username.
     */
    static String getMedusaUsername() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_DLS_MEDUSA_USERNAME");
    }

    /**
     * @return HTTP Basic secret.
     */
    static String getMedusaSecret() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_DLS_MEDUSA_SECRET");
    }

    static String getServiceKey() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_DLS_KEY");
    }

    /**
     * Fetches a collection from {@literal /collections/:id}.
     */
    static MedusaCollection fetchMedusaCollection(String uri) throws IOException {
        LOGGER.debug("Fetching Medusa collection: {}", uri);

        try {
            ContentResponse response = getMedusaClient().newRequest(uri)
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
        try {
            if (dlsClient != null) {
                dlsClient.stop();
            }
        } catch (Exception e) {
            LOGGER.error("close(): " + e.getMessage());
        } finally {
            try {
                if (medusaClient != null) {
                    medusaClient.stop();
                }
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

    private static synchronized HttpClient getDLSClient() {
        if (dlsClient == null) {
            dlsClient = new HttpClient(new SslContextFactory());
            dlsClient.setFollowRedirects(true);
            try {
                dlsClient.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return dlsClient;
    }

    private static synchronized HttpClient getMedusaClient() {
        if (medusaClient == null) {
            medusaClient = new HttpClient(new SslContextFactory());
            medusaClient.setFollowRedirects(true);
            try {
                final String username = getMedusaUsername();
                final String secret   = getMedusaSecret();
                if (username != null && secret != null) {
                    AuthenticationStore auth = medusaClient.getAuthenticationStore();
                    auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                            new URI(getMedusaEndpointURI()), username, secret));
                }
                medusaClient.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return medusaClient;
    }

    @Override
    public int numEntities() throws IOException {
        if (numEntities < 0) {
            // Get and count published Medusa collections.
            if (collections.isEmpty()) {
                fetchMedusaCollections();
            }
            numEntities = collections.size();

            // Add the count of items from the DLS.
            String uri = getDLSHarvestURI();

            if (lastModified != null) {
                uri += "?last_modified_after=" + lastModified.getEpochSecond();
            }

            try {
                ContentResponse response = getDLSClient()
                        .newRequest(uri)
                        .header("Accept", "application/json")
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                String body = response.getContentAsString();
                JSONObject jobj = new JSONObject(body);
                numEntities += jobj.getInt("numResults");
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
    public ConcurrentIterator<? extends Entity> entities() throws IOException {
        // Get published Medusa collections.
        if (collections.isEmpty()) {
            fetchMedusaCollections();
        }
        final AtomicInteger collectionsIndex = new AtomicInteger();

        // Queue of DLS entity URIs.
        final Queue<String> batch = new ConcurrentLinkedQueue<>();
        final AtomicInteger batchIndex = new AtomicInteger();

        // Return an iterator that first consumes the set of Medusa collections,
        // and then the queue of DLS entities.
        return new ConcurrentIterator<>() {
            @Override
            public Entity next() throws Exception {
                final int collectionIndex = collectionsIndex.getAndIncrement();
                if (collectionIndex < collections.size()) {
                    return collections.get(collectionIndex);
                }

                // If the queue is empty, fetch the next batch.
                synchronized (this) {
                    if (batch.peek() == null) {
                        fetchDLSBatch(batch, batchIndex.getAndIncrement());
                    }
                }

                try {
                    return fetchDLSEntity(batch.remove());
                } catch (NoSuchElementException e) {
                    throw new EndOfIterationException();
                }
            }
        };
    }

    /**
     * Fetches the list of published collections from Medusa.
     */
    private void fetchMedusaCollections() throws IOException {
        final String uri = String.format("%s/collections.json",
                getMedusaEndpointURI());
        LOGGER.debug("Fetching Medusa collections: {}", uri);

        collections.clear();

        try {
            ContentResponse response = getMedusaClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            if (response.getStatus() == 200) {
                String body = response.getContentAsString();

                JSONArray results = new JSONArray(body);
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jcol = results.getJSONObject(i);
                    String collectionURI = getMedusaEndpointURI() +
                            jcol.getString("path") + ".json";
                    MedusaCollection col = fetchMedusaCollection(collectionURI);
                    if (col.isDLS() && col.isPublished()) {
                        collections.add(col);
                    }
                }
                LOGGER.debug("Fetched {} collections", collections.size());
            } else {
                throw new IOException("Got HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param batch      Queue of entity URIs.
     * @param batchIndex Zero-based batch index.
     */
    private void fetchDLSBatch(final Queue<String> batch,
                               final int batchIndex) throws IOException {
        if (isClosed.get()) {
            LOGGER.debug("fetchDLSBatch(): stopping");
            return;
        }

        final int numResults = numEntities();
        final int numBatches = (int) Math.ceil(numResults / (double) BATCH_SIZE);
        final int offset = batchIndex * BATCH_SIZE;

        String uri = String.format("%s?start=%d&limit=%d",
                getDLSHarvestURI(), offset, BATCH_SIZE);
        if (lastModified != null) {
            uri += "&last_modified_after=" + lastModified.getEpochSecond();
        }
        LOGGER.debug("Fetching DLS batch {} of {}: {}",
                batchIndex + 1, numBatches, uri);

        try {
            ContentResponse response = getDLSClient().newRequest(uri)
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

        LOGGER.debug("Fetched {} DLS results", batch.size());
    }

    private Entity fetchDLSEntity(String uri) throws IOException {
        LOGGER.debug("Fetching DLS entity: {}", uri);
        try {
            ContentResponse response = getDLSClient().newRequest(uri)
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
