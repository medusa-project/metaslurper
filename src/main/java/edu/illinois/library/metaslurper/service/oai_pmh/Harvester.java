package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import edu.illinois.library.metaslurper.service.IterationException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private abstract class AbstractIterator<T> {

        final AtomicInteger numEntities = new AtomicInteger(-1);
        final Queue<T> batch = new ConcurrentLinkedQueue<>();

        private final AtomicInteger index = new AtomicInteger();
        private String resumptionToken;

        /**
         * @param resumptionToken Current resumption token. Will be {@literal
         *                        null} at the beginning of the harvest.
         * @param batch           Queue to put harvested results into.
         * @return                Next resumption token. Will be {@literal
         *                        null} at the end of the harvest.
         */
        abstract String fetchBatch(String resumptionToken,
                                   Queue<T> batch) throws IOException;

        Document fetchDocument(String uri) throws IOException {
            InputStreamResponseListener responseListener =
                    new InputStreamResponseListener();

            LOGGER.debug("fetchDocument(): requesting {}", uri);

            getClient().newRequest(uri)
                    .timeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .send(responseListener);

            try {
                // Wait for the response headers to arrive.
                Response response = responseListener.get(
                        REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (response.getStatus() == HttpStatus.OK_200) {
                    final DocumentBuilderFactory factory =
                            DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    final DocumentBuilder builder = factory.newDocumentBuilder();

                    try (InputStream is = responseListener.getInputStream()) {
                        return builder.parse(is);
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

        public T next() throws EndOfIterationException, IterationException {
            if (numEntities.get() >= 0 && index.incrementAndGet() >= numEntities.get()) {
                throw new EndOfIterationException();
            }

            // If the queue is empty, fetch the next batch.
            synchronized (this) {
                if (batch.peek() == null) {
                    try {
                        resumptionToken = fetchBatch(resumptionToken, batch);
                    } catch (IOException e) {
                        throw new IterationException(e);
                    }
                }
            }

            return batch.remove();
        }

    }

    private class RecordIterator<T> extends AbstractIterator<T>
            implements ConcurrentIterator<T> {
        @Override
        String fetchBatch(String resumptionToken,
                          Queue<T> batch) throws IOException {
            if (numEntities.get() < 0) {
                numEntities.set(numRecords());
            }

            String uri;
            if (resumptionToken != null) {
                uri = String.format("%s?verb=ListRecords&resumptionToken=%s",
                        endpointURI, URLEncoder.encode(resumptionToken, "UTF-8"));
            } else {
                uri = String.format("%s?verb=ListRecords&metadataPrefix=%s",
                        endpointURI, metadataPrefix);
            }

            final Document doc = fetchDocument(uri);
            final XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xpath = xPathFactory.newXPath();
            xpath.setNamespaceContext(new OAINamespaceContext());

            try {
                // Transform each <record> element into a PMHRecord and add
                // it to the batch queue.
                XPathExpression expr = xpath.compile("//oai:record");

                final NodeList nodes = (NodeList) expr.evaluate(
                        doc, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); i++) {
                    final Node recordNode = nodes.item(i);
                    final PMHRecord record = new PMHRecord();
                    // identifier
                    XPathExpression recordExpr = xpath.compile(
                            "oai:header/oai:identifier");
                    record.setIdentifier(recordExpr.evaluate(recordNode));
                    // datestamp
                    recordExpr = xpath.compile("oai:header/oai:datestamp");
                    record.setDatestamp(recordExpr.evaluate(recordNode));
                    // setSpec
                    recordExpr = xpath.compile("oai:header/oai:setSpec");
                    record.setSetSpec(recordExpr.evaluate(recordNode));
                    // metadata
                    recordExpr = xpath.compile("oai:metadata/*/*");
                    NodeList mdnodes = (NodeList) recordExpr.evaluate(
                            recordNode, XPathConstants.NODESET);
                    for (int j = 0; j < mdnodes.getLength(); j++) {
                        Node mdnode = mdnodes.item(j);
                        Element e = new Element(mdnode.getNodeName(),
                                mdnode.getTextContent());
                        if (e.getValue() != null && !e.getValue().isEmpty()) {
                            record.getElements().add(e);
                        }
                    }
                    batch.add((T) record);
                }

                // Pluck out the resumptionToken and return it.
                expr = xpath.compile("//oai:resumptionToken");
                return expr.evaluate(doc);
            } catch (XPathExpressionException e) {
                throw new IOException(e);
            }
        }
    }

    private class SetIterator<T> extends AbstractIterator<T>
            implements ConcurrentIterator<T> {
        @Override
        String fetchBatch(String resumptionToken,
                          Queue<T> batch) throws IOException {
            if (numEntities.get() < 0) {
                numEntities.set(numSets());
            }

            String uri;
            if (resumptionToken != null) {
                uri = String.format("%s?verb=ListSets&resumptionToken=%s",
                        endpointURI, URLEncoder.encode(resumptionToken, "UTF-8"));
            } else {
                uri = String.format("%s?verb=ListSets", endpointURI);
            }

            final Document doc = fetchDocument(uri);
            final XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xpath = xPathFactory.newXPath();
            xpath.setNamespaceContext(new OAINamespaceContext());

            try {
                // Transform each <record> element into a PMHRecord and add
                // it to the batch queue.
                XPathExpression expr = xpath.compile("//oai:set");

                final NodeList nodes = (NodeList) expr.evaluate(
                        doc, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); i++) {
                    final Node setNode = nodes.item(i);
                    final PMHSet set = new PMHSet();
                    // spec
                    XPathExpression setExpr = xpath.compile("oai:setSpec");
                    set.setSpec(setExpr.evaluate(setNode));
                    // name
                    setExpr = xpath.compile("oai:setName");
                    set.setName(setExpr.evaluate(setNode));
                    // metadata
                    setExpr = xpath.compile("oai:setDescription/*/*");
                    NodeList mdnodes = (NodeList) setExpr.evaluate(
                            setNode, XPathConstants.NODESET);
                    for (int j = 0; j < mdnodes.getLength(); j++) {
                        Node mdnode = mdnodes.item(j);
                        Element e = new Element(mdnode.getNodeName(),
                                mdnode.getTextContent());
                        if (e.getValue() != null && !e.getValue().isEmpty()) {
                            set.getElements().add(e);
                        }
                    }
                    batch.add((T) set);
                }

                // Pluck out the resumptionToken and return it.
                expr = xpath.compile("//oai:resumptionToken");
                return expr.evaluate(doc);
            } catch (XPathExpressionException e) {
                throw new IOException(e);
            }
        }
    }

    private static final class OAINamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix != null) {
                switch (prefix) {
                    case "dc":
                        return "http://purl.org/dc/elements/1.1/";
                    case "oai_dc":
                        return "http://www.openarchives.org/OAI/2.0/oai_dc/";
                    case "oai":
                        return "http://www.openarchives.org/OAI/2.0/";
                    default:
                        return XMLConstants.NULL_NS_URI;
                }
            }
            throw new NullPointerException("Null prefix");
        }

        @Override
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Harvester.class);

    private static final String DEFAULT_METADATA_PREFIX = "oai_dc";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

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

    public ConcurrentIterator<PMHRecord> records() {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }

        return new RecordIterator<>();
    }

    public ConcurrentIterator<PMHSet> sets() {
        if (endpointURI == null) {
            throw new IllegalStateException("Endpoint URI is not set");
        }

        return new SetIterator<>();
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
