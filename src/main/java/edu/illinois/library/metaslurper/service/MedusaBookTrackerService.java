package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.ConfigurationFactory;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Dolski UIUC
 */
final class MedusaBookTrackerService implements SourceService {

    private static class BookTrackerEntity implements Entity {

        private JSONObject rootObject;

        private BookTrackerEntity(JSONObject rootObject) {
            this.rootObject = rootObject;
        }

        @Override
        public String getAccessImageURI() {
            return null;
        }

        @Override
        public Set<Element> getElements() {
            final Set<Element> elements = new HashSet<>();

            for (String key : new String[] { "author", "bib_id", "created_at",
                    "date", "ia_identifier", "obj_id", "oclc_number", "title",
                    "updated_at" }) {
                if (rootObject.has(key)) {
                    String value = rootObject.get(key).toString();

                    // If the object contains a `volume` key, append it to the
                    // title.
                    if ("title".equals(key) && rootObject.has("volume")) {
                        value += " " + rootObject.get("volume");
                    }

                    if (!value.isEmpty() && !"null".equals(value)) {
                        elements.add(new Element(key, value));
                    }
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
            Configuration config = ConfigurationFactory.getConfiguration();
            return config.getString("service.source.medusa_book_tracker.key");
        }

        @Override
        public String getSinkID() {
            final String key = "id";
            return rootObject.has(key) ?
                    getServiceKey() + "-" + rootObject.getInt(key) : null;
        }

        @Override
        public String getSourceID() {
            final String key = "id";
            return rootObject.has(key) ?
                    Integer.toString(rootObject.getInt(key)) : null;
        }

        @Override
        public String getSourceURI() {
            final String key = "url";
            return rootObject.has(key) ? rootObject.getString(key) : null;
        }

        @Override
        public Variant getVariant() {
            return Variant.BOOK;
        }

        @Override
        public String toString() {
            return getSourceID() + " / " + getSinkID();
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaBookTrackerService.class);

    private static final String NAME = "MedusaBookTracker";

    private static final long REQUEST_TIMEOUT = 30;

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private int numEntities = -1, windowSize = -1;

    private static String getEndpointURI() {
        Configuration config = ConfigurationFactory.getConfiguration();
        String endpoint = config.getString("service.source.medusa_book_tracker.endpoint");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
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
            fetchNumEntities();
        }
        return numEntities;
    }

    private void fetchNumEntities() throws IOException {
        try {
            ContentResponse response = getClient()
                    .newRequest(getEndpointURI() + "/items")
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            String body = response.getContentAsString();
            JSONObject jobj = new JSONObject(body);
            numEntities = jobj.getInt("numResults");
            windowSize = jobj.getInt("windowSize");
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public ConcurrentIterator<Entity> entities() {
        final Queue<Entity> batch = new ConcurrentLinkedQueue<>();
        final AtomicInteger pageNumber = new AtomicInteger(1);

        // Return an iterator that consumes the queue.
        return new ConcurrentIterator<Entity>() {
            @Override
            public Entity next() throws Exception {
                // If the queue is empty, fetch the next batch.
                synchronized (this) {
                    if (batch.peek() == null) {
                        fetchBatch(batch, pageNumber.getAndIncrement());
                    }
                }

                if (batch.peek() == null) {
                    throw new EndOfIterationException();
                }

                return batch.remove();
            }
        };
    }

    private void fetchBatch(final Queue<Entity> batch,
                            final int pageNumber) throws IOException {
        if (isClosed.get()) {
            LOGGER.debug("fetchBatch(): stopping");
            return;
        }

        final int numResults = numEntities();
        final int numPages = (int) Math.ceil(numResults / (float) windowSize);

        final String uri = String.format("%s/items?page=%d",
                getEndpointURI(), pageNumber);
        LOGGER.debug("Fetching {} results (page {} of {}): {}",
                windowSize, pageNumber + 1, numPages, uri);

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
                    jobj = jarr.getJSONObject(i);
                    batch.add(new BookTrackerEntity(jobj));
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

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
