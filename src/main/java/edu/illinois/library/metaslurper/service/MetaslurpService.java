package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.ConfigurationFactory;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import org.apache.commons.configuration2.Configuration;
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

/**
 * @see <a href="https://github.com/medusa-project/metaslurp">Metaslurp
 *   GitHub</a>
 */
final class MetaslurpService implements SinkService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaslurpService.class);

    private static final String NAME = "Metaslurp";

    private HttpClient client;

    private static URI getEndpointURI() {
        Configuration config = ConfigurationFactory.getConfiguration();
        String endpoint = config.getString("service.sink.endpoint");
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String getUsername() {
        Configuration config = ConfigurationFactory.getConfiguration();
        return config.getString("service.sink.username");
    }

    private static String getSecret() {
        Configuration config = ConfigurationFactory.getConfiguration();
        return config.getString("service.sink.secret");
    }

    private static URI getURI(Entity entity) {
        final URI uri = getEndpointURI();
        switch (entity.getType()) {
            case COLLECTION:
                return uri.resolve("/api/v1/collections/" + entity.getID());
            default:
                return uri.resolve("/api/v1/items/" + entity.getID());
        }
    }

    private HttpClient getClient() {
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
    public String getName() {
        return NAME;
    }

    @Override
    public void ingest(Entity entity) throws IOException {
        final URI uri = getURI(entity);
        final String json = toJSON(entity);

        LOGGER.debug("Ingesting {}: {}", entity, json);
        try {
            ContentResponse response = getClient()
                    .newRequest(uri)
                    .method(HttpMethod.PUT)
                    .header("Content-Type", "application/json")
                    .content(new StringContentProvider(json), "application/json")
                    .send();
            if (response.getStatus() != HttpStatus.NO_CONTENT_204) {
                throw new IOException("Received HTTP " + response.getStatus() +
                        " from " + uri);
            }
        } catch (InterruptedException | ExecutionException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    private String toJSON(Entity entity) {
        JSONObject jobj = new JSONObject();
        // ID
        jobj.put("index_id", entity.getID());
        // service key
        jobj.put("service_key", entity.getServiceKey());
        // source URI
        jobj.put("source_uri", entity.getSourceURI());
        // access image URI
        jobj.put("access_image_uri", entity.getAccessImageURI());
        // elements
        JSONArray jelements = new JSONArray();
        for (Element element : entity.getElements()) {
            JSONObject jelement = new JSONObject();
            jelement.put("name", element.getName());
            jelement.put("value", element.getValue());
            jelements.put(jelement);
        }
        jobj.put("elements", jelements);

        return jobj.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
