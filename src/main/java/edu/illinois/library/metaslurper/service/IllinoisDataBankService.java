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

/**
 * @author Alex Dolski UIUC
 */
final class IllinoisDataBankService implements SourceService {

    /**
     * Data set backed by a JSON object from a {@literal /datasets/:id}
     * representation.
     */
    private static class DataSet implements Entity {

        private JSONObject rootObject;

        private DataSet(JSONObject rootObject) {
            this.rootObject = rootObject;
        }

        @Override
        public Set<Element> getElements() {
            final Set<Element> elements = new HashSet<>();

            for (String key : new String[] { "corresponding_creator_name",
                    "created_at", "description", "keywords", "license",
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
            Configuration config = ConfigurationFactory.getConfiguration();
            return config.getString("service.source.databank.key");
        }

        @Override
        public String getSinkID() {
            return getServiceKey() + "-" +
                    getSourceID().replaceAll("[^A-Za-z\\d]", "_");
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

    private static final String NAME = "IllinoisDataBank";

    private static final long REQUEST_TIMEOUT = 60; // IDB can be slow sometimes...

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    /**
     * Populated by {@link #fetchDataSetURIs()}.
     */
    private final Queue<String> dataSetURIs = new ConcurrentLinkedQueue<>();

    private static String getEndpointURI() {
        Configuration config = ConfigurationFactory.getConfiguration();
        String endpoint = config.getString("service.source.databank.endpoint");
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

    private void checkClosed() {
        if (isClosed.get()) {
            throw new IllegalStateException("Instance is closed");
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
    public synchronized int numEntities() throws IOException {
        checkClosed();

        if (dataSetURIs.isEmpty()) {
            fetchDataSetURIs();
        }
        return dataSetURIs.size();
    }

    @Override
    public synchronized ConcurrentIterator<Entity> entities()
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
            if (response.getStatus() == 200) {
                String body = response.getContentAsString();

                JSONArray results = new JSONArray(body);
                for (int i = 0; i < results.length(); i++) {
                    JSONObject dataSet = results.getJSONObject(i);
                    dataSetURIs.add(dataSet.getString("url"));
                }
                LOGGER.debug("Fetched {} data sets", dataSetURIs.size());
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
            if (response.getStatus() == 200) {
                String body = response.getContentAsString();
                JSONObject jobj = new JSONObject(body);
                return jobj.getBoolean("is_test") ? null : new DataSet(jobj);
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
    public String toString() {
        return getClass().getSimpleName();
    }

}
