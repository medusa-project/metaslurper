package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
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

    private static class BookTrackerEntity implements ConcreteEntity {

        /**
         * JSON keys that will be expressed as {@link #getElements() elements}.
         */
        private static final String[] ELEMENTS = {
                "author", "bib_id", "catalog_url", "created_at", "date",
                "hathitrust_rights", "hathitrust_url",
                "internet_archive_identifier", "internet_archive_url",
                "language", "obj_id", "oclc_number", "subjects", "title",
                "updated_at" };

        private JSONObject rootObject;

        private BookTrackerEntity(JSONObject rootObject) {
            this.rootObject = rootObject;
        }

        @Override
        public Set<Element> getElements() {
            final Set<Element> elements = new HashSet<>();

            for (String key : ELEMENTS) {
                if (rootObject.has(key)) {
                    if (rootObject.get(key) instanceof JSONArray) {
                        JSONArray array = rootObject.getJSONArray(key);
                        for (int i = 0; i < array.length(); i++) {
                            String value = array.get(i).toString().trim();
                            if (!value.isEmpty() && !"null".equals(value)) {
                                elements.add(new Element(key, value));
                            }
                        }
                    } else {
                        String value = rootObject.get(key).toString().trim();

                        // If the object contains a non-null `volume` key,
                        // append it to the title.
                        if ("title".equals(key)) {
                            String volume = rootObject.get("volume").toString().trim();
                            if (!volume.isEmpty() && !"null".equals(volume)) {
                                value += " " + volume;
                            }
                        }

                        if (!value.isEmpty() && !"null".equals(value)) {
                            elements.add(new Element(key, value));
                        }
                    }
                }
            }
            return elements;
        }

        @Override
        public String getServiceKey() {
            return getKeyFromConfiguration();
        }

        @Override
        public String getSinkID() {
            return getServiceKey() + "-" + getSourceID();
        }

        @Override
        public String getSourceID() {
            return Integer.toString(rootObject.getInt("id"));
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

    private static final String NAME = "Medusa Book Tracker";

    private static final String QUERY_FILTER = "harvest=true";

    private static final long REQUEST_TIMEOUT = 30;

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private int numEntities = -1, windowSize = -1;

    private Instant lastModified;

    private static String getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_BOOK_TRACKER_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private static String getKeyFromConfiguration() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_BOOK_TRACKER_KEY");
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
            fetchNumEntities();
        }
        return numEntities;
    }

    private void fetchNumEntities() throws IOException {
        try {
            String uri = getEndpointURI() + "/items?" + QUERY_FILTER;
            if (lastModified != null) {
                uri += "&last_modified_after=" + lastModified.getEpochSecond();
            }

            ContentResponse response = getClient()
                    .newRequest(uri)
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
    public ConcurrentIterator<? extends Entity> entities() {
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

        String uri = String.format("%s/items?page=%d&%s",
                getEndpointURI(), pageNumber, QUERY_FILTER);
        if (lastModified != null) {
            uri += "&last_modified_after=" + lastModified.getEpochSecond();
        }
        LOGGER.debug("Fetching {} results (page {} of {}): {}",
                windowSize, pageNumber, numPages, uri);

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
