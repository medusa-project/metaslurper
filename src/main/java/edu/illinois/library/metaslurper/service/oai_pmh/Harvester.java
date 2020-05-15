package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
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
import java.time.Instant;
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
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private OkHttpClient client;

    private String endpointURI;
    private String metadataPrefix = DEFAULT_METADATA_PREFIX;

    private Instant from, until;

    private synchronized OkHttpClient getClient() {
        if (client == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

    /**
     * @return Total number of records available via the endpoint.
     */
    public int numRecords() throws IOException {
        String uri = String.format("%s?verb=ListIdentifiers&metadataPrefix=%s",
                endpointURI, metadataPrefix);
        if (from != null) {
            uri += "&from=" + from;
        }
        if (until != null) {
            uri += "&until=" + until;
        }
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

        LOGGER.debug("fetchCountFromListResponse(): requesting {}", uri);

        Request.Builder builder = new Request.Builder()
                .method("GET", null)
                .url(uri);
        Request request = builder.build();
        Response response = getClient().newCall(request).execute();
        if (response.code() == 200) {
            try (ResponseBody body = response.body();
                 InputStream is = body.byteStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = factory.newDocumentBuilder();
                Document doc = docBuilder.parse(is);
                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                XPathExpression expr =
                        xpath.compile("//resumptionToken/@completeListSize");
                int value;
                try {
                    value = Integer.parseInt(expr.evaluate(doc));
                } catch (NumberFormatException e) {
                    expr  = xpath.compile("count(//" + elementToCount + ")");
                    value = Integer.parseInt(expr.evaluate(doc));
                }
                return value;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            throw new IOException("Received HTTP " + response.code() +
                    " for " + uri);
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
                from, until, numRecords(), tx);
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public void setUntil(Instant until) {
        this.until = until;
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
