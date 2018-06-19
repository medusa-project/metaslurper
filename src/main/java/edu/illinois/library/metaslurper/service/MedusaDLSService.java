package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.async.ThreadPool;
import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Dolski UIUC
 */
final class MedusaDLSService implements SourceService {

    /**
     * N.B.: DLS item and collection JSON representations are more-or-less the
     * same.
     */
    private static class DLSEntity implements Entity {

        private JSONObject rootObject;

        private DLSEntity(JSONObject rootObject) {
            this.rootObject = rootObject;
        }

        @Override
        public String getAccessImageURI() {
            final String key = "effective_representative_image_uri";
            try {
                return rootObject.has(key) ? rootObject.getString(key) : null;
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Set<Element> getElements() {
            final String key = "elements";
            final Set<Element> elements = new HashSet<>();
            if (rootObject.has(key)) {
                final JSONArray jelements = rootObject.getJSONArray(key);

                for (int i = 0; i < jelements.length(); i++) {
                    JSONObject jelement = jelements.getJSONObject(i);
                    String name = jelement.getString("name");
                    String value = jelement.getString("value");
                    Element element = new Element(name, value);
                    elements.add(element);
                }
            }
            return elements;
        }

        @Override
        public String getMediaType() {
            return null;
        }

        @Override
        public String getServiceKey() {
            return getKeyFromConfiguration();
        }

        @Override
        public String getSinkID() {
            final String key = "id";
            return rootObject.has(key) ?
                    ENTITY_ID_PREFIX + rootObject.getString(key) : null;
        }

        @Override
        public String getSourceID() {
            final String key = "id";
            return rootObject.has(key) ? rootObject.getString(key) : null;
        }

        @Override
        public String getSourceURI() {
            final String key = "public_uri";
            return rootObject.has(key) ? rootObject.getString(key) : null;
        }

        @Override
        public Variant getVariant() {
            final String key = "class";
            if (rootObject.has(key)) {
                switch (rootObject.getString(key)) {
                    case "Collection":
                        return Variant.COLLECTION;
                    default:
                        return Variant.ITEM;
                }
            }
            return Variant.UNKNOWN;
        }

        @Override
        public String toString() {
            return getSourceID() + " / " + getSinkID();
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaDLSService.class);

    /**
     * N.B.: 100 is the maximum the DLS allows.
     */
    private static final int BATCH_SIZE = 100;

    static final String ENTITY_ID_PREFIX = "dls-";

    private static final String NAME = "Illinois Digital Library";

    private static final long REQUEST_TIMEOUT = 30;

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    /**
     * Queue of item and collection URIs.
     */
    private final BlockingQueue<String> resultsQueue =
            new LinkedBlockingQueue<>();

    private int numItems = -1, numCollections = -1;

    private static String getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_DLS_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private static String getCollectionsURI() {
        return getEndpointURI() + "/collections";
    }

    private static String getItemsURI() {
        return getEndpointURI() + "/items";
    }

    private static String getKeyFromConfiguration() {
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
        return numCollections() + numItems();
    }

    private int numCollections() throws IOException {
        if (numCollections < 0) {
            numCollections = fetchNumEntities(getCollectionsURI());
        }
        return numCollections;
    }

    private int numItems() throws IOException {
        if (numItems < 0) {
            numItems = fetchNumEntities(getItemsURI());
        }
        return numItems;
    }

    /**
     * Fetches the item count into {@link #numItems}.
     */
    private int fetchNumEntities(String endpoint) throws IOException {
        try {
            ContentResponse response = getClient()
                    .newRequest(endpoint)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            String body = response.getContentAsString();
            JSONObject jobj = new JSONObject(body);
            return jobj.getInt("numResults");
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    /**
     * Provides a stream of entities. A producer thread fetches results list
     * pages, parses them, and feeds the entity URIs they contain into a queue.
     * Entity representations are fetched on-demand as the client consumes the
     * stream.
     */
    @Override
    public ConcurrentIterator<Entity> entities() throws IOException {
        final AtomicBoolean shouldAbort = new AtomicBoolean();
        final int numItems = numItems();
        final int numCollections = numCollections();

        LOGGER.debug("{} collections and {} items to fetch",
                numCollections, numItems);

        // Start a separate thread to load results page-by-page into a queue.
        ThreadPool.getInstance().submit(() -> {
            try {
                fetchAndQueueResults(getCollectionsURI(), numCollections);
                fetchAndQueueResults(getItemsURI(), numItems);
            } catch (IOException e) {
                shouldAbort.set(true);
                throw new UncheckedIOException(e);
            }
        });

        // Return an iterator that consumes the queue.
        return new ConcurrentIterator<Entity>() {
            private final AtomicInteger index = new AtomicInteger();

            @Override
            public Entity next() throws Exception {
                if (shouldAbort.get()) {
                    throw new Exception("Aborting prematurely. " +
                            "Something probably went wrong in a results response.");
                }
                try {
                    if (index.getAndIncrement() < numItems + numCollections) {
                        String uri = resultsQueue.take();
                        return fetchEntity(uri);
                    } else {
                        throw new EndOfIterationException();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (InterruptedException e) {
                    throw new UncheckedIOException(new IOException(e));
                }
            }
        };
    }

    private void fetchAndQueueResults(final String endpointURI,
                                      final int numResults) throws IOException {
        final int numPages = (int) Math.ceil(numResults / (float) BATCH_SIZE);

        for (int page = 0; page < numPages; page++) {
            if (isClosed.get()) {
                LOGGER.debug("fetchAndQueueResults(): stopping");
                return;
            }
            final String uri = String.format("%s?start=%d&limit=%d",
                    endpointURI, page * BATCH_SIZE, BATCH_SIZE);
            LOGGER.debug("Fetching {} results (page {} of {}): {}",
                    BATCH_SIZE, page + 1, numPages, uri);

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
                        String juri = jarr.getJSONObject(i).getString("uri");
                        resultsQueue.add(juri);
                    }
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + uri);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException e) {
                throw new IOException(e);
            }
        }
        LOGGER.debug("Fetched {} results", numResults);
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
                    case 200:
                        String body = response.getContentAsString();
                        return new DLSEntity(new JSONObject(body));
                    default:
                        body = response.getContentAsString();
                        JSONObject jobj = new JSONObject(body);
                        String message = jobj.getString("error");

                        throw new IOException("Got HTTP " + response.getStatus() +
                                " for " + uri + ": " + message);
                }
            } else {
                throw new IOException("Unsupported response Content-Type: " +
                        response.getMediaType());
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
