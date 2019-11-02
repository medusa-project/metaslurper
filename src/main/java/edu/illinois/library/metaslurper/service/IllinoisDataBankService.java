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

/**
 * @author Alex Dolski UIUC
 */
final class IllinoisDataBankService implements SourceService {

    /**
     * Data set backed by a JSON object from a {@literal /datasets/:id}
     * representation.
     */
    private static class DataSet implements ConcreteEntity {

        private JSONObject rootObject;

        private DataSet(JSONObject rootObject) {
            this.rootObject = rootObject;
        }

        @Override
        public Set<Element> getElements() {
            final Set<Element> elements = new HashSet<>();

            // service name
            elements.add(new Element("service", PUBLIC_NAME));

            // keywords
            if (rootObject.has("keywords")) {
                String value = rootObject.get("keywords").toString();
                if (!value.isBlank() && !"null".equals(value)) {
                    String[] parts = value.split(";");
                    for (String s : parts) {
                        String part = s.trim();
                        if (!part.isBlank()) {
                            elements.add(new Element("keywords", part));
                        }
                    }
                }
            }

            // all other elements
            for (String key : new String[] { "corresponding_creator_name",
                    "created_at", "description", "license",
                    "publication_state", "publication_year", "publisher",
                    "release_date", "title", "updated_at" }) {
                if (rootObject.has(key)) {
                    String value = rootObject.get(key).toString().trim();
                    if (!value.isEmpty() && !"null".equals(value)) {
                        elements.add(new Element(key, value));
                    }
                }
            }

            // Add another element containing binary filenames.
            JSONArray dataFiles = rootObject.getJSONArray("datafiles");
            for (int i = 0; i < dataFiles.length(); i++) {
                JSONObject dataFile = dataFiles.getJSONObject(i);
                elements.add(new Element("binary_name",
                        dataFile.getString("binary_name")));
            }

            return elements;
        }

        @Override
        public String getServiceKey() {
            return getKeyFromConfiguration();
        }

        @Override
        public String getSourceID() {
            return rootObject.getString("identifier");
        }

        @Override
        public String getSourceURI() {
            final String key = "url";
            if (rootObject.has(key)) {
                String url = rootObject.getString(key);
                // Trim off the ".json"
                if (url.endsWith(".json")) {
                    url = url.substring(0, url.length() - 5);
                }
                return url;
            }
            return null;
        }

        @Override
        public String getSinkID() {
            return getServiceKey() + "-" +
                    getSourceID().replaceAll("[^A-Za-z\\d]", "_");
        }

        @Override
        public String getParentSinkID() {
            return null;
        }

        @Override
        public String getContainerSinkID() {
            return null;
        }

        @Override
        public String getContainerName() {
            return null;
        }

        @Override
        public Variant getVariant() {
            return Variant.DATA_SET;
        }

        @Override
        public String toString() {
            return getSourceID() + " / " + getSinkID();
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IllinoisDataBankService.class);

    private static final String PRIVATE_NAME = "Illinois Data Bank";
    private static final String PUBLIC_NAME  = "Research Data";

    private static final long REQUEST_TIMEOUT = 60;

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    /**
     * Populated by {@link #fetchDataSetURIs()}.
     */
    private final Queue<String> dataSetURIs = new ConcurrentLinkedQueue<>();

    private static String getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_IDB_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private static String getKeyFromConfiguration() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_IDB_KEY");
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
    public String getKey() {
        return getKeyFromConfiguration();
    }

    @Override
    public String getName() {
        return PRIVATE_NAME;
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
    public synchronized int numEntities() throws IOException {
        checkClosed();

        if (dataSetURIs.isEmpty()) {
            fetchDataSetURIs();
        }
        return dataSetURIs.size();
    }

    @Override
    public synchronized ConcurrentIterator<? extends Entity> entities()
            throws IOException {
        checkClosed();

        if (dataSetURIs.isEmpty()) {
            fetchDataSetURIs();
        }

        return () -> {
            if (dataSetURIs.peek() == null) {
                throw new EndOfIterationException();
            }

            DataSet dataSet;
            do {
                dataSet = fetchDataSet(dataSetURIs.remove());
            } while (dataSet == null);
            return dataSet;
        };
    }

    /**
     * Fetches all data set URIs from {@literal /datasets} into {@link
     * #dataSetURIs}. (We don't want to use the data set representations at
     * {@literal /datasets} because the ones at {@literal /datasets/:id} are
     * more detailed.)
     */
    private void fetchDataSetURIs() throws IOException {
        final String uri = String.format("%s/datasets", getEndpointURI());
        LOGGER.debug("Fetching data sets: {}", uri);
        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            String body = response.getContentAsString();
            if (response.getStatus() == 200) {
                JSONArray results = new JSONArray(body);
                for (int i = 0; i < results.length(); i++) {
                    JSONObject dataSet = results.getJSONObject(i);
                    dataSetURIs.add(dataSet.getString("url"));
                }
                LOGGER.debug("Fetched {} data sets", dataSetURIs.size());
            } else {
                throw new HTTPException(
                        "GET", uri, response.getStatus(), null, body);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new HTTPException("GET", uri, e);
        }
    }

    /**
     * Fetches and returns the data set at the given URI.
     *
     * @return Data set, or {@literal null} if the data set at the given URI is
     *         a test data set.
     */
    private DataSet fetchDataSet(String uri) throws IOException {
        LOGGER.debug("Fetching data set: {}", uri);
        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            String body = response.getContentAsString();
            if (response.getStatus() == 200) {
                JSONObject jobj = new JSONObject(body);
                return jobj.getBoolean("is_test") ? null : new DataSet(jobj);
            } else {
                throw new HTTPException(
                        "GET", uri, response.getStatus(), null, body);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new HTTPException("GET", uri, e);
        }
    }

    @Override
    public void setLastModified(Instant lastModified) {
        // This service doesn't support incremental harvesting, but that's OK
        // since it contains so little content.
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
