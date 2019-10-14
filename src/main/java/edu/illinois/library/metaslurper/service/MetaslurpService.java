package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.harvest.HarvestClosedException;
import edu.illinois.library.metaslurper.harvest.Status;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @see <a href="https://github.com/medusa-project/metaslurp">Metaslurp
 *   GitHub</a>
 */
final class MetaslurpService implements SinkService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaslurpService.class);

    private static final String NAME = "Metaslurp";

    private HttpClient client;
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private MetaslurpHarvest harvest;
    private int numEntities;

    private static URI getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SINK_METASLURP_ENDPOINT");
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String getUsername() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SINK_METASLURP_USERNAME");
    }

    private static String getSecret() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SINK_METASLURP_SECRET");
    }

    private static URI getURI(Entity entity) {
        return getEndpointURI().resolve("/api/v1/items/" + entity.getSinkID());
    }

    private synchronized HttpClient getClient() {
        if (client == null) {
            client = new HttpClient(new SslContextFactory());
            client.setFollowRedirects(true);
            try {
                if (getUsername() != null && getSecret() != null) {
                    AuthenticationStore auth = client.getAuthenticationStore();
                    auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                            getEndpointURI(), getUsername(), getSecret()));
                }
                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return client;
    }

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
    public String getKey() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SINK_METASLURP_KEY");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setNumEntitiesToIngest(int numEntitiesToIngest) {
        this.numEntities = numEntitiesToIngest;
    }

    @Override
    public void ingest(ConcreteEntity entity) throws IOException {
        if (isClosed.get()) {
            throw new IllegalStateException("Instance is closed.");
        }

        synchronized (this) {
            if (harvest == null) {
                final String harvestKey =
                        System.getenv("SERVICE_SINK_METASLURP_HARVEST_KEY");
                if (harvestKey != null && !harvestKey.isEmpty()) {
                    harvest = new MetaslurpHarvest(harvestKey, numEntities);
                } else {
                    createHarvest(entity.getServiceKey());
                }
            }
        }

        final URI uri = getURI(entity);
        final String json = toJSON(entity);

        LOGGER.debug("Ingesting {} {}: {}",
                entity.getVariant().name().toLowerCase(), entity, json);
        try {
            ContentResponse response = getClient()
                    .newRequest(uri)
                    .method(HttpMethod.PUT)
                    .header("Content-Type", "application/json")
                    .content(new StringContentProvider(json), "application/json")
                    .send();
            switch (response.getStatus()) {
                case HttpStatus.NO_CONTENT_204: // success
                    break;
                case 480: // harvest ended (this should never happen)
                    throw new HarvestClosedException(
                            "Harvest " + harvest + " is no longer available.");
                case 481: // harvest aborted
                    throw new HarvestClosedException(
                            "Harvest " + harvest + " has been aborted.");
                default:
                    throw new IOException("Received HTTP " + response.getStatus() +
                            " for PUT " + uri + "\n" +
                            "Request body: " + json + "\n" +
                            "Response body: " + response.getContentAsString());
            }
        } catch (InterruptedException | ExecutionException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    /**
     * Populates {@link #harvest}.
     */
    private void createHarvest(final String serviceKey) throws IOException {
        final URI uri = getEndpointURI().resolve("/api/v1/harvests");
        final String json = "{\"service_key\":\"" + serviceKey + "\"}";

        LOGGER.debug("Creating harvest: {}", json);
        try {
            ContentResponse response = getClient()
                    .newRequest(uri)
                    .method(HttpMethod.POST)
                    .header("Content-Type", "application/json")
                    .content(new StringContentProvider(json), "application/json")
                    .send();
            if (response.getStatus() == HttpStatus.CREATED_201) {
                String entity = response.getContentAsString();
                JSONObject entityJSON = new JSONObject(entity);
                harvest = new MetaslurpHarvest(
                        entityJSON.getString("key"), numEntities);
            } else {
                throw new IOException("Received HTTP " + response.getStatus() +
                        " for POST " + uri + "\n" +
                        "Request body: " + json + "\n" +
                        "Response body: " + response.getContentAsString());
            }
        } catch (InterruptedException | ExecutionException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void updateStatus(final Status status) throws IOException {
        if (harvest == null) {
            return;
        }
        harvest.setStatus(status);
        final URI uri = getEndpointURI().resolve(harvest.getPath());
        final String json = harvest.toJSON();

        LOGGER.debug("Updating status of {}: {}", uri, json);
        try {
            ContentResponse response = getClient()
                    .newRequest(uri)
                    .method("PATCH")
                    .header("Content-Type", "application/json")
                    .content(new StringContentProvider(json), "application/json")
                    .send();
            if (response.getStatus() != HttpStatus.NO_CONTENT_204) {
                throw new IOException("Received HTTP " + response.getStatus() +
                        " for PATCH " + uri + "\n" +
                        "Request body: " + json + "\n" +
                        "Response body: " + response.getContentAsString());
            }
        } catch (InterruptedException | ExecutionException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    private String toJSON(ConcreteEntity entity) {
        JSONObject jobj = new JSONObject();
        // harvest key
        jobj.put("harvest_key", harvest.getKey());
        // variant
        jobj.put("variant", toString(entity.getVariant()));
        // media type
        jobj.put("media_type", entity.getMediaType());
        // source ID
        jobj.put("source_id", entity.getSourceID());
        // sink ID
        jobj.put("id", entity.getSinkID());
        // parent sink ID
        jobj.put("parent_id", entity.getParentSinkID());
        // container sink ID
        jobj.put("container_id", entity.getContainerSinkID());
        // container name
        jobj.put("container_name", entity.getContainerName());
        // service key
        jobj.put("service_key", entity.getServiceKey());
        // source URI
        jobj.put("source_uri", entity.getSourceURI());

        // access images
        JSONArray jimages = new JSONArray();
        entity.getAccessImages().forEach(image -> {
            JSONObject jimage = new JSONObject();
            jimage.put("crop", image.getCrop().name().toLowerCase());
            jimage.put("size", (image.getSize() > 0) ? image.getSize() : "full");
            jimage.put("uri", image.getURI());
            jimage.put("master", image.isMaster());
            jimages.put(jimage);
        });
        jobj.put("images", jimages);

        // elements
        JSONArray jelements = new JSONArray();
        entity.getElements().forEach(element -> {
            JSONObject jelement = new JSONObject();
            jelement.put("name", element.getName());
            jelement.put("value", element.getValue());
            jelements.put(jelement);
        });
        jobj.put("elements", jelements);

        return jobj.toString();
    }

    private static String toString(Variant variant) {
        switch (variant) {
            case BOOK:
                return "Book";
            case COLLECTION:
                return "Collection";
            case DATA_SET:
                return "DataSet";
            case NEWSPAPER_PAGE:
                return "NewspaperPage";
            case PAPER:
                return "Paper";
            default:
                return "Item";
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
