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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

final class MedusaDLSService implements SourceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaDLSService.class);

    private static final int DEFAULT_BATCH_SIZE    = 200;
    private static final int DEFAULT_THROTTLE_MSEC = 500;

    private static final String NAME = "DLS";

    private HttpClient client;

    /**
     * Queue of item URIs.
     */
    private final BlockingQueue<String> resultsQueue =
            new LinkedBlockingQueue<>();

    private int numItems = -1;

    @Override
    public void close() {
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

    private int getThrottleMsec() {
        Configuration config = ConfigurationFactory.getConfiguration();
        return config.getInt("service.source.medusa_dls.throttle_msec",
                DEFAULT_THROTTLE_MSEC);
    }

    @Override
    public int numItems() {
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
    public Stream<Item> items() {
        final int numItems = numItems();

        LOGGER.debug("{} items to fetch", numItems);

        ThreadPool.getInstance().submit(() -> fetchAndQueueResults(numItems));

        return Stream.generate(() -> {
            try {
                String uri = resultsQueue.take();
                throttle();
                return fetchItem(uri);
            } catch (InterruptedException | ExecutionException |
                    TimeoutException e) {
                throw new UncheckedIOException(new IOException(e));
            }
        }).limit(numItems);
    }

    /**
     * Fetches the item count into {@link #numItems}.
     */
    private void fetchNumItems() {
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
            LOGGER.error("fetchNumItems(): {}", e.getMessage(), e);
        }
    }

    private void fetchAndQueueResults(int numResults) {
        final int batchSize = getBatchSize();

        for (int page = 0; page < Math.ceil(numResults / batchSize); page++) {
            final String url = String.format("%s?start=%d&limit=%d",
                    getEndpointURI(), page * batchSize, batchSize);
            LOGGER.debug("Fetching {} results: {}", batchSize, url);

            try {
                ContentResponse response = getClient().newRequest(url)
                        .header("Accept", "application/json")
                        .send();
                String body = response.getContentAsString();
                JSONObject jobj = new JSONObject(body);
                JSONArray jarr = jobj.getJSONArray("results");
                for (int i = 0; i < jarr.length(); i++) {
                    String itemURI = jarr.getJSONObject(i).getString("uri");
                    resultsQueue.add(itemURI);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException e) {
                LOGGER.error("fetchAndQueueResults(): ", e.getMessage(), e);
            }
            throttle();
        }

        LOGGER.debug("Done fetching results");
    }

    private Item fetchItem(String itemURI) throws ExecutionException,
            InterruptedException, TimeoutException {
        LOGGER.debug("Fetching item: {}", itemURI);

        ContentResponse response = getClient().newRequest(itemURI)
                .header("Accept", "application/json")
                .send();
        String body = response.getContentAsString();
        try {
            JSONObject jobj = new JSONObject(body);

            Item item = new Item(jobj.getString("id"));

            JSONArray jelements = jobj.getJSONArray("elements");

            for (int i = 0; i < jelements.length(); i++) {
                JSONObject jelement = jelements.getJSONObject(i);
                String name = jelement.getString("name");
                String value = jelement.getString("value");
                Element element = new Element(name, value);
                item.getElements().add(element);
            }
            return item;
        } catch (JSONException e) {
            if (body.substring(0, 20).trim().startsWith("<!DOCTYPE")) {
                LOGGER.warn("fetchItem(): received HTML for URI: {}", itemURI);
            } else {
                LOGGER.warn("fetchItem(): Invalid JSON for URI: {}\nResponse body: {}",
                        itemURI, body);
            }
        }
        return null;
    }

    private void throttle() {
        try {
            Thread.sleep(getThrottleMsec());
        } catch (InterruptedException e) {
            LOGGER.warn("throttle(): ", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
