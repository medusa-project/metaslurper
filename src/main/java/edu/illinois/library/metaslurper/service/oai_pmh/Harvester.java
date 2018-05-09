package edu.illinois.library.metaslurper.service.oai_pmh;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Generic OAI-PMH metadata harvester. Uses SAX parsing for streaming
 * response handling.</p>
 *
 * <h1>Usage:</h1>
 *
 * <pre>
 * Harvester h = new Harvester();
 * h.setEndpointURI("http://...");
 * h.setListener(new Listener() { ... });
 * h.harvest();
 * </pre>
 *
 * <h1>Notes</h1>
 *
 * <ul>
 *     <li>{@literal setSpec} elements are mapped to {@literal dc:identifier}
 *     elements.</li>
 *     <li>{@literal setName} elements are mapped to {@literal dc:title}
 *     elements.</li>
 * </ul>
 *
 * @author Alex Dolski UIUC
 */
public final class Harvester implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Harvester.class);

    private static final String DEFAULT_METADATA_PREFIX = "oai_dc";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private HttpClient client;

    private String endpointURI;
    private String metadataPrefix = DEFAULT_METADATA_PREFIX;

    private Listener listener;

    private static XMLReader newXMLReader() {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser parser = spf.newSAXParser();
            return parser.getXMLReader();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
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
    public int getNumRecords() throws IOException {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }
        String uri = String.format(
                "%s?verb=ListIdentifiers&metadataPrefix=%s",
                endpointURI, metadataPrefix);
        InputStreamResponseListener responseListener =
                new InputStreamResponseListener();

        LOGGER.debug("getNumRecords(): requesting {}", uri);

        getClient().newRequest(uri)
                .timeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .send(responseListener);

        try {
            // Wait for the response headers to arrive.
            Response response = responseListener.get(
                    REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                XMLReader reader = newXMLReader();
                ListRecordsResponseHandler handler =
                        new ListRecordsResponseHandler();
                reader.setContentHandler(handler);

                try (InputStream is = responseListener.getInputStream()) {
                    reader.parse(new InputSource(is));
                }
                return handler.getCompleteListSize();
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

    /**
     * @return Total number of sets available via the endpoint.
     */
    public int getNumSets() throws IOException {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }
        String uri = String.format("%s?verb=ListSets", endpointURI);
        InputStreamResponseListener responseListener =
                new InputStreamResponseListener();

        LOGGER.debug("getNumSets(): requesting {}", uri);

        getClient().newRequest(uri)
                .timeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .send(responseListener);

        try {
            // Wait for the response headers to arrive.
            Response response = responseListener.get(
                    REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                XMLReader reader = newXMLReader();
                ListSetsResponseHandler handler = new ListSetsResponseHandler();
                reader.setContentHandler(handler);

                try (InputStream is = responseListener.getInputStream()) {
                    reader.parse(new InputSource(is));
                }
                return handler.getCompleteListSize();
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

    /**
     * @throws IllegalStateException if {@link #setEndpointURI(String)} and
     *                               {@link #setListener(Listener)} have not
     *                               been called.
     */
    public void harvest() throws IOException {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        } else if (listener == null) {
            throw new IllegalStateException("Listener is not set");
        }
        harvestSets(null);
        harvestRecords(null);
    }

    /**
     * @param resumptionToken May be {@literal null}.
     */
    private void harvestRecords(String resumptionToken) throws IOException {
        String uri;
        if (resumptionToken != null) {
            uri = String.format("%s?verb=ListRecords&resumptionToken=%s",
                    endpointURI, resumptionToken);
        } else {
            uri = String.format("%s?verb=ListRecords&metadataPrefix=%s",
                    endpointURI, metadataPrefix);
        }

        InputStreamResponseListener responseListener =
                new InputStreamResponseListener();

        LOGGER.debug("harvestRecords(): requesting {}", uri);

        getClient().newRequest(uri).send(responseListener);

        try {
            // Wait for the response headers to arrive.
            Response response = responseListener.get(
                    REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                XMLReader reader = newXMLReader();
                ListRecordsResponseHandler handler =
                        new ListRecordsResponseHandler();
                handler.setListener(listener);
                reader.setContentHandler(handler);

                try (InputStream is = responseListener.getInputStream()) {
                    reader.parse(new InputSource(is));
                }
                String nextResumptionToken = handler.getResumptionToken();
                if (nextResumptionToken != null) {
                    harvestRecords(nextResumptionToken);
                }
            } else {
                throw new IOException("Received HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (InterruptedException | TimeoutException |
                ExecutionException | SAXException | OAIPMHException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param resumptionToken May be {@literal null}.
     */
    private void harvestSets(String resumptionToken) throws IOException {
        String uri;
        if (resumptionToken != null) {
            uri = String.format("%s?verb=ListSets&resumptionToken=%s",
                    endpointURI, resumptionToken);
        } else {
            uri = String.format("%s?verb=ListSets", endpointURI);
        }

        InputStreamResponseListener responseListener =
                new InputStreamResponseListener();

        LOGGER.debug("harvestSets(): requesting {}", uri);

        getClient().newRequest(uri).send(responseListener);

        try {
            // Wait for the response headers to arrive.
            Response response = responseListener.get(
                    REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                XMLReader reader = newXMLReader();
                ListSetsResponseHandler handler =
                        new ListSetsResponseHandler();
                handler.setListener(listener);
                reader.setContentHandler(handler);

                try (InputStream is = responseListener.getInputStream()) {
                    reader.parse(new InputSource(is));
                }
                String nextResumptionToken = handler.getResumptionToken();
                if (nextResumptionToken != null) {
                    harvestSets(nextResumptionToken);
                }
            } else {
                throw new IOException("Received HTTP " + response.getStatus() +
                        " for " + uri);
            }
        } catch (InterruptedException | TimeoutException |
                ExecutionException | SAXException | OAIPMHException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param uri OAI-PMH endpoint URI.
     */
    public void setEndpointURI(String uri) {
        this.endpointURI = uri;
    }

    /**
     * @param listener Object to listen for events generated by {@link
     *                 #harvest()}.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
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
