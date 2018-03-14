package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.async.ThreadPool;
import edu.illinois.library.metaslurper.config.ConfigurationFactory;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Item;
import org.apache.commons.configuration2.Configuration;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
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
     * N.B.: 100 is the maximum the DLS allows.
     */
    private static final int DEFAULT_BATCH_SIZE = 100;

    static final String ITEM_ID_PREFIX = "dls-";

    private static final String NAME = "DLS";

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    /**
     * Queue of item URIs.
     */
    private final BlockingQueue<String> resultsQueue =
            new LinkedBlockingQueue<>();

    private int numItems = -1;

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

    private HttpClient getClient() {
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

    private int getBatchSize() {
        Configuration config = ConfigurationFactory.getConfiguration();
        return config.getInt("service.source.medusa_dls.batch_size",
                DEFAULT_BATCH_SIZE);
    }

    private String getEndpointURI() {
        Configuration config = ConfigurationFactory.getConfiguration();
        String endpoint = config.getString("service.source.medusa_dls.endpoint");
        return (endpoint.endsWith("/")) ?
                endpoint + "items" : endpoint + "/items";
    }

    @Override
    public int numItems() throws IOException {
        if (numItems < 0) {
            fetchNumItems();
        }
        return numItems;
    }

    /**
     * Provides a stream of items. A producer thread fetches results list
     * pages, parses them, and feeds the item URIs they contain into a queue.
     * Item representations are fetched on-demand as the client consumes the
     * stream.
     */
    @Override
    public ConcurrentIterator<Item> items() throws IOException {
        final int numItems = numItems();

        LOGGER.debug("{} items to fetch", numItems);

        // Start a separate thread to load results page-by-page into a queue.
        ThreadPool.getInstance().submit(() -> {
            try {
                fetchAndQueueResults(numItems);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // Return an iterator that consumes the queue.
        return new ConcurrentIterator<Item>() {
            private final AtomicInteger index = new AtomicInteger();

            @Override
            public Item next() throws EndOfIterationException {
                try {
                    if (index.getAndIncrement() < numItems) {
                        String uri = resultsQueue.take();
                        return fetchItem(uri);
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

    /**
     * Fetches the item count into {@link #numItems}.
     */
    private void fetchNumItems() throws IOException {
        try {
            ContentResponse response = getClient()
                    .newRequest(getEndpointURI())
                    .header("Accept", "application/json")
                    .send();
            String body = response.getContentAsString();
            JSONObject jobj = new JSONObject(body);
            numItems = jobj.getInt("numResults");
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    private void fetchAndQueueResults(int numResults) throws IOException {
        final int batchSize = getBatchSize();
        final int numPages = (int) Math.ceil(numResults / batchSize);

        for (int page = 0; page < numPages; page++) {
            if (isClosed.get()) {
                LOGGER.debug("fetchAndQueueResults(): stopping");
                return;
            }
            final String url = String.format("%s?start=%d&limit=%d",
                    getEndpointURI(), page * batchSize, batchSize);
            LOGGER.debug("Fetching {} results (page {} of {}): {}",
                    batchSize, page + 1, numPages, url);

            try {
                ContentResponse response = getClient().newRequest(url)
                        .header("Accept", "application/json")
                        .send();
                if (response.getStatus() == 200) {
                    String body = response.getContentAsString();
                    JSONObject jobj = new JSONObject(body);
                    JSONArray jarr = jobj.getJSONArray("results");
                    for (int i = 0; i < jarr.length(); i++) {
                        String itemURI = jarr.getJSONObject(i).getString("uri");
                        resultsQueue.add(itemURI);
                    }
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + url);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException e) {
                throw new IOException(e);
            }
        }
        LOGGER.debug("Done fetching results");
    }

    private Item fetchItem(String itemURI) throws IOException {
        LOGGER.debug("Fetching item: {}", itemURI);
        try {
            ContentResponse response = getClient().newRequest(itemURI)
                    .header("Accept", "application/json")
                    .send();
            if ("application/json".equals(response.getMediaType())) {
                switch (response.getStatus()) {
                    case 200:
                        String body = response.getContentAsString();
                        try {
                            JSONObject jobj = new JSONObject(body);

                            Item item = new Item(ITEM_ID_PREFIX + jobj.getString("id"));
                            item.setSourceURI(new URI(jobj.getString("public_uri")));

                            JSONArray jelements = jobj.getJSONArray("elements");

                            for (int i = 0; i < jelements.length(); i++) {
                                JSONObject jelement = jelements.getJSONObject(i);
                                String name = jelement.getString("name");
                                String value = jelement.getString("value");
                                Element element = new Element(name, value);
                                item.getElements().add(element);
                            }
                            return item;
                        } catch (URISyntaxException e) {
                            LOGGER.warn("fetchItem(): Invalid public_uri value for item: {}",
                                    itemURI);
                        }
                        break;
                    default:
                        body = response.getContentAsString();
                        JSONObject jobj = new JSONObject(body);
                        String message = jobj.getString("error");

                        throw new IOException("Got HTTP " + response.getStatus() +
                                " for " + itemURI + ": " + message);
                }
            } else {
                throw new IOException("Unsupported response Content-Type: " +
                        response.getMediaType());
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
