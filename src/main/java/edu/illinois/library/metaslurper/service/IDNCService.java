package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.util.NumberUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Service for the <a href="http://idnc.library.illinois.edu">Illinois
 * Digital Newspaper Collections</a>.</p>
 *
 * <p>This service uses an undocumented incremental harvesting feature of
 * Veridian which UIUC requested for this purpose.</p>
 *
 * @see <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/">
 *     Veridian XML API Documentation</a>
 */
final class IDNCService implements SourceService {

    /**
     * Entity backed by a
     * <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent">
     * GetPageContent</a> representation.
     */
    private static final class IDNCEntity implements ConcreteEntity {

        private Document doc;

        private IDNCEntity(String xml) throws IOException,
                ParserConfigurationException, SAXException {
            try (Reader reader = new StringReader(xml)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(new InputSource(reader));
            }
        }

        @Override
        public String getAccessImageURI() {
            // This is a full-sized image, likely several thousand pixels on a
            // side. GIF, JPEG, and PNG are available.
            // N.B.: width= and height= arguments are also available. The full
            // dimensions are encoded in sub-elements of the /Page/PageMetadata
            // element of a GetPageContent response.
            return String.format("%s/cgi-bin/imageserver.pl?oid=%s&color=all&ext=jpg",
                    getEndpointURI(), getSourceID());
        }

        @Override
        public Set<Element> getElements() {
            final Set<Element> elements = new HashSet<>();

            String date = string("//DocumentMetadata/DocumentDate");
            if (!date.isEmpty()) {
                elements.add(new Element("date", date));
            }

            String fullText = string("//PageTextHTML");
            if (!fullText.isEmpty()) {
                elements.add(new Element("fullText", fullText));
            }

            String pubTitle = string("//PublicationMetadata/PublicationTitle");
            if (!pubTitle.isEmpty()) {
                elements.add(new Element("publicationTitle", pubTitle));
            }

            String title = string("//PageMetadata/PageTitle");
            if (!title.isEmpty()) {
                elements.add(new Element("title", pubTitle + ", " + date + " - " + title));
            }

            String prevID = string("//PagePrevPageID");
            if (!prevID.isEmpty()) {
                elements.add(new Element("previousPageID", prevID));
            }

            String nextID = string("//PageNextPageID");
            if (!nextID.isEmpty()) {
                elements.add(new Element("nextPageID", nextID));
            }
            return elements;
        }

        @Override
        public String getServiceKey() {
            return getKeyFromConfiguration();
        }

        @Override
        public String getSinkID() {
            return ENTITY_ID_PREFIX + getSourceID().replace(".", "_");
        }

        @Override
        public String getSourceID() {
            return string("//PageMetadata/PageID");
        }

        @Override
        public String getSourceURI() {
            return getEndpointURI() + string("//PageViewURL");
        }

        @Override
        public Variant getVariant() {
            return Variant.NEWSPAPER_PAGE;
        }

        @Override
        public String toString() {
            return getSourceID();
        }

        private String string(final String xpathQuery) {
            try {
                XPathFactory factory = XPathFactory.newInstance();
                XPath xpath = factory.newXPath();
                XPathExpression expr = xpath.compile(xpathQuery);
                String result = (String) expr.evaluate(doc, XPathConstants.STRING);
                if (result != null) {
                    return result;
                }
            } catch (XPathExpressionException e) {
                LOGGER.error(e.getMessage());
            }
            return "";
        }

    }

    private final class PageIterator implements ConcurrentIterator<Entity> {

        /**
         * Queue of document IDs within the current result window.
         */
        private final Queue<String> docBatch      = new ConcurrentLinkedQueue<>();
        private final AtomicInteger docBatchIndex = new AtomicInteger();
        private final AtomicInteger docIndex      = new AtomicInteger();

        private int numDocuments;

        /**
         * Used for reporting the average number of pages per document.
         */
        private final List<Short> numPagesPerDocument =
                Collections.synchronizedList(new LinkedList<>());

        /**
         * Queue of page IDs within the current document.
         */
        private final Queue<String> pageBatch = new ConcurrentLinkedQueue<>();

        @Override
        public Entity next() throws Exception {
            // If the page queue is empty, fetch the next batch of pages.
            synchronized (this) {
                if (pageBatch.peek() == null) {
                    fetchPageBatch();
                }
            }

            if (pageBatch.peek() == null || isClosed.get()) {
                throw new EndOfIterationException();
            }

            return fetchPage(pageBatch.remove());
        }

        /**
         * Fetches all of the next document's pages into {@link #pageBatch}.
         */
        private void fetchPageBatch() throws IOException {
            synchronized (this) {
                if (docBatch.peek() == null) {
                    fetchDocumentBatch();
                }
            }

            if (docBatch.peek() == null) {
                return;
            }

            try {
                final String docID = docBatch.remove();

                // See: https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getdocumentcontent
                // Example: http://idnc.library.illinois.edu/cgi-bin/illinois?a=d&d=CHP19370109&f=XML
                final String uri = String.format(
                        "%s/cgi-bin/illinois?a=d&d=%s&f=XML",
                        getEndpointURI(), docID);

                LOGGER.debug("Fetching document {}/{} ({}): {}",
                        docIndex.incrementAndGet(), numDocuments,
                        NumberUtils.percent(docIndex.get(), numDocuments),
                        uri);

                ContentResponse response = getClient()
                        .newRequest(uri)
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                if (response.getStatus() == 200) {
                    byte[] entity = response.getContent();
                    try (ByteArrayInputStream is = new ByteArrayInputStream(entity)) {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = dbFactory.newDocumentBuilder();
                        Document doc = builder.parse(is);

                        XPathFactory xpFactory = XPathFactory.newInstance();
                        XPath xpath = xpFactory.newXPath();
                        XPathExpression expr = xpath.compile("//PageID");
                        NodeList pageIDs = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                        for (int i = 0; i < pageIDs.getLength(); i++) {
                            pageBatch.add(pageIDs.item(i).getTextContent());
                        }

                        numPagesPerDocument.add((short) pageBatch.size());

                        LOGGER.debug("Fetched {} page IDs from document {} ({} average pages per document)",
                                pageBatch.size(), docID,
                                getAverageNumPagesPerDocument());
                    }
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + uri);
                }
            } catch (NoSuchElementException e) {
                // done
            } catch (ExecutionException | InterruptedException |
                    TimeoutException | ParserConfigurationException |
                    SAXException | XPathExpressionException e) {
                throw new IOException(e);
            }
        }

        private double getAverageNumPagesPerDocument() {
            return numPagesPerDocument.stream()
                    .mapToInt(Short::intValue)
                    .average()
                    .orElse(0.0);
        }

        /**
         * Fetches the next {@link #RESULT_WINDOW_SIZE} document IDs into
         * {@link #docBatch}.
         */
        private void fetchDocumentBatch() throws IOException {
            // See: https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#searchdocuments
            // Example: http://idnc.library.illinois.edu/cgi-bin/illinois?a=q&leq=Document&f=XML&o=100&r=101&sf=byDA
            final String uri = String.format(
                    "%s/cgi-bin/illinois?a=q&leq=Document&f=XML&o=%d&r=%d&sf=byDA",
                    getEndpointURI(), RESULT_WINDOW_SIZE, docBatchIndex.get() + 1);

            LOGGER.debug("Fetching documents {}-{}: {}",
                    docBatchIndex.get() + 1,
                    docBatchIndex.get() + 1 + RESULT_WINDOW_SIZE,
                    uri);

            try {
                ContentResponse response = getClient()
                        .newRequest(uri)
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                if (response.getStatus() == 200) {
                    byte[] entity = response.getContent();
                    try (ByteArrayInputStream is = new ByteArrayInputStream(entity)) {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(is);

                        XPathFactory factory = XPathFactory.newInstance();
                        XPath xpath = factory.newXPath();

                        XPathExpression expr =
                                xpath.compile("//TotalNumberOfSearchResults");
                        numDocuments = Integer.parseInt(
                                (String) expr.evaluate(doc, XPathConstants.STRING));

                        expr = xpath.compile("//DocumentID");
                        NodeList docIDs = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                        for (int i = 0; i < docIDs.getLength(); i++) {
                            docBatch.add(docIDs.item(i).getTextContent());
                            docBatchIndex.getAndIncrement();
                        }
                        LOGGER.debug("Fetched {} document IDs", docBatch.size());
                    }
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + uri);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException | ParserConfigurationException |
                    SAXException | XPathExpressionException e) {
                throw new IOException(e);
            }
        }

        private Entity fetchPage(final String id) throws IOException {
            // See: https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent
            // Example: http://idnc.library.illinois.edu/cgi-bin/illinois?a=d&d=CHP19370109.1.4&f=XML
            final String uri = String.format(
                    "%s/cgi-bin/illinois?a=d&d=%s&f=XML",
                    getEndpointURI(), id);
            LOGGER.debug("Fetching page: {}", uri);

            try {
                ContentResponse response = getClient()
                        .newRequest(uri)
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                if (response.getStatus() == 200) {
                    String entity = response.getContentAsString();
                    return new IDNCEntity(entity);
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + uri);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException | ParserConfigurationException |
                    SAXException e) {
                throw new IOException(e);
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IDNCService.class);

    static final String ENTITY_ID_PREFIX = "idnc-";

    private static final String NAME = "IDNC";

    private static final int REQUEST_TIMEOUT = 60;

    /**
     * 100 is the largest window allowed by the Veridian XML API.
     */
    private static final int RESULT_WINDOW_SIZE = 100;

    private HttpClient client;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    /**
     * YYYYMMDD format
     */
    private int lastModified;

    private synchronized HttpClient getClient() {
        if (client == null) {
            client = new HttpClient();
            client.setFollowRedirects(true);
            try {
                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return client;
    }

    /**
     * @return Base URI of the service.
     */
    private static String getEndpointURI() {
        Configuration config = Configuration.getInstance();
        String endpoint = config.getString("SERVICE_SOURCE_IDNC_ENDPOINT");
        return (endpoint.endsWith("/")) ?
                endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private static String getKeyFromConfiguration() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_IDNC_KEY");
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

    @Override
    public String getKey() {
        return getKeyFromConfiguration();
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * This isn't supported because it would require 140,000+ HTTP requests.
     *
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public int numEntities() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConcurrentIterator<? extends Entity> entities() {
        if (lastModified > 0) {
            return incrementalEntities();
        }
        return allEntities();
    }

    private ConcurrentIterator<? extends Entity> allEntities() {
        return new PageIterator();
    }

    private ConcurrentIterator<? extends Entity> incrementalEntities() {
        // TODO: support incremental harvests
        return new PageIterator();
    }

    @Override
    public void setLastModified(Instant lastModified) {
        Date date = Date.from(lastModified);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        this.lastModified = Integer.parseInt(formatter.format(date));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
