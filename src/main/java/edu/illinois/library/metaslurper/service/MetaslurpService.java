package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.harvest.HarvestClosedException;
import edu.illinois.library.metaslurper.harvest.Harvest;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @see <a href="https://github.com/medusa-project/metaslurp">Metaslurp
 *   GitHub</a>
 */
final class MetaslurpService implements SinkService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaslurpService.class);

    private static final String NAME = "Metaslurp";
    private static final long REQUEST_TIMEOUT = 30;

    private OkHttpClient client;
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

    private static String getIndex() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SINK_METASLURP_INDEX");
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

    private synchronized OkHttpClient getClient() {
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

    @Override
    public void close() {
        if (client != null) {
            // If OkHttp isn't shut down manually, it will keep the app running
            // for a time after a harvest instead of immediately exiting.
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
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
    public void setSourceKey(String sourceKey) throws IOException {
        // Create a harvest.
        // The harvest key environment variable here will be:
        //
        // 1. Non-null, typically indicating that a harvest has been started by
        //    the counterpart web app and its key passed into the container/VM
        //    in which this app is running, as the value of this environment
        //    variable.
        // 2. Null if a harvest has not yet been created, which will typically
        //    be the case when the application is invoked directly via the
        //    command line.
        final String harvestKey =
                System.getenv("SERVICE_SINK_METASLURP_HARVEST_KEY");
        if (harvestKey != null && !harvestKey.isEmpty()) { // (1)
            harvest = new MetaslurpHarvest(harvestKey, numEntities);
        } else { // (2)
            createHarvest(sourceKey);
        }
    }

    @Override
    public void ingest(ConcreteEntity entity) throws IOException {
        if (isClosed.get()) {
            throw new IllegalStateException("Instance is closed.");
        }

        final String uri = getURI(entity).toString();
        final String json = toJSON(entity);

        LOGGER.debug("Ingesting {} {}: {}",
                entity.getVariant().name().toLowerCase(), entity, json);

        Request.Builder builder = new Request.Builder()
                .header("Accept", "application/json")
                .put(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.get("application/json");
                    }
                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {
                        bufferedSink.writeString(json, StandardCharsets.UTF_8);
                    }
                })
                .url(uri);
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();

        switch (response.code()) {
            case 204: // success
                break;
            case 480: // harvest ended (this should never happen)
                throw new HarvestClosedException(
                        "Harvest " + harvest + " is no longer available.");
            case 481: // harvest aborted
                throw new HarvestClosedException(
                        "Harvest " + harvest + " has been aborted.");
            default:
                throw new HTTPException("PUT",
                        uri, response.code(), json, response.body().string());
        }
    }

    /**
     * Populates {@link #harvest}.
     */
    private void createHarvest(final String serviceKey) throws IOException {
        if (harvest != null) {
            throw new RuntimeException("A harvest already exists. " +
                    "This is probably a bug.");
        }
        final String uri = getEndpointURI().resolve("/api/v1/harvests").toString();
        final String json = "{\"service_key\":\"" + serviceKey + "\"}";

        LOGGER.debug("Creating harvest: {}", json);

        Request.Builder builder = new Request.Builder()
                .header("Accept", "application/json")
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.get("application/json");
                    }
                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {
                        bufferedSink.writeString(json, StandardCharsets.UTF_8);
                    }
                })
                .url(uri);
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();
        String entity = response.body().string();

        if (response.code() == 201) {
            JSONObject entityJSON = new JSONObject(entity);
            harvest = new MetaslurpHarvest(
                    entityJSON.getString("key"), numEntities);
        } else {
            throw new HTTPException("POST",
                    uri, response.code(), json, entity);
        }
    }

    @Override
    public void updateHarvest(final Harvest harvest) throws IOException {
        if (this.harvest == null) {
            return;
        }
        this.harvest.setHarvest(harvest);
        final String uri = getEndpointURI().resolve(this.harvest.getPath()).toString();
        final String json = this.harvest.toJSON();

        LOGGER.debug("Updating status of {}: {}", uri, json);

        Request.Builder builder = new Request.Builder()
                .header("Accept", "application/json")
                .patch(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.get("application/json");
                    }
                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {
                        bufferedSink.writeString(json, StandardCharsets.UTF_8);
                    }
                })
                .url(uri);
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();

        if (response.code() != 204) {
            throw new HTTPException("PATCH",
                    uri.toString(),
                    response.code(),
                    json,
                    response.body().string());
        }

    }

    private String toJSON(ConcreteEntity entity) {
        JSONObject jobj = new JSONObject();
        // container sink ID
        jobj.put("container_id", entity.getContainerSinkID());
        // container name
        jobj.put("container_name", entity.getContainerName());

        // elements
        JSONArray jelements = new JSONArray();
        entity.getElements().forEach(element -> {
            JSONObject jelement = new JSONObject();
            jelement.put("name", element.getName());
            jelement.put("value", element.getValue());
            jelements.put(jelement);
        });
        jobj.put("elements", jelements);

        // harvest key
        jobj.put("harvest_key", harvest.getKey());

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

        // index
        if (getIndex() != null) {
            jobj.put("id", getIndex());
        }
        // sink ID
        jobj.put("id", entity.getSinkID());
        // media type
        jobj.put("media_type", entity.getMediaType());
        // parent sink ID
        jobj.put("parent_id", entity.getParentSinkID());
        // service key
        jobj.put("service_key", entity.getServiceKey());
        // source ID
        jobj.put("source_id", entity.getSourceID());
        // source URI
        jobj.put("source_uri", entity.getSourceURI());
        // variant
        jobj.put("variant", toString(entity.getVariant()));

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
