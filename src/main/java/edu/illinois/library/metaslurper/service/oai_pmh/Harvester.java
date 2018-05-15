package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * <p>Generic pull-based OAI-PMH harvester.</p>
 *
 * <h1>Usage:</h1>
 *
 * <pre>
 * Harvester h = new Harvester();
 * h.setEndpointURI("http://...");
 *
 * int numSets = h.numSets();
 * int numRecords = h.numRecords();
 *
 * ConcurrentIterator&lt;PMHSet&gt; it = h.sets();
 * while (true) {
 *     try {
 *         PMHSet set = it.next();
 *     } catch (EndOfIterationException e) {
 *         break;
 *     }
 * }
 * </pre>
 *
 * @author Alex Dolski UIUC
 */
public final class Harvester implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Harvester.class);

    private static final String DEFAULT_METADATA_PREFIX = "oai_dc";
    static final int REQUEST_TIMEOUT_SECONDS = 30;

    private HttpClient client;

    private String endpointURI;
    private String metadataPrefix = DEFAULT_METADATA_PREFIX;

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
    public void close() {
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                LOGGER.error("close(): " + e.getMessage());
            }
        }
    }

    /**
     * @return Total number of records available via the endpoint.
     */
    public int numRecords() throws IOException {
        String uri = String.format("%s?verb=ListIdentifiers&metadataPrefix=%s",
                endpointURI, metadataPrefix);
        return fetchCountFromListResponse(uri, "header");
    }

    /**
     * @return Total number of sets available via the endpoint.
     */
    public int numSets() throws IOException {
        String uri = String.format("%s?verb=ListSets", endpointURI);
        return fetchCountFromListResponse(uri, "set");
    }

    private int fetchCountFromListResponse(final String uri,
                                           final String elementToCount) throws IOException {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }

        InputStreamResponseListener responseListener =
                new InputStreamResponseListener();

        LOGGER.debug("fetchCountFromListResponse(): requesting {}", uri);

        getClient().newRequest(uri)
                .timeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .send(responseListener);

        try {
            // Wait for the response headers to arrive.
            Response response = responseListener.get(
                    REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                try (InputStream is = responseListener.getInputStream()) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(is);
                    XPathFactory xPathfactory = XPathFactory.newInstance();
                    XPath xpath = xPathfactory.newXPath();
                    XPathExpression expr =
                            xpath.compile("//resumptionToken/@completeListSize");
                    int value;
                    try {
                        value = Integer.parseInt(expr.evaluate(doc));
                    } catch (NumberFormatException e) {
                        expr = xpath.compile("count(//" + elementToCount + ")");
                        value = Integer.parseInt(expr.evaluate(doc));
                    }
                    return value;
                }
            } else {
                throw new IOException("Received HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public ConcurrentIterator<PMHRecord> records() throws IOException {
        return records(new DefaultElementTransformer());
    }

    public ConcurrentIterator<PMHRecord> records(ElementTransformer tx) throws IOException {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }

        return new RecordIterator<>(getClient(), endpointURI, metadataPrefix,
                numRecords(), tx);
    }

    public ConcurrentIterator<PMHSet> sets() throws IOException {
        return sets(new DefaultElementTransformer());
    }

    public ConcurrentIterator<PMHSet> sets(ElementTransformer tx) throws IOException {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }

        return new SetIterator<>(getClient(), endpointURI, numSets(), tx);
    }

    /**
     * @param uri OAI-PMH endpoint URI.
     */
    public void setEndpointURI(String uri) {
        this.endpointURI = uri;
    }

    /**
     * If this method is not called, {@link #DEFAULT_METADATA_PREFIX} will be
     * used.
     *
     * @param metadataPrefix Metadata format to use.
     */
    public void setMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }

}
