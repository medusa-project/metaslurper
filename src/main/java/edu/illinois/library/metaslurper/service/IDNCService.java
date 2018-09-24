package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Service for the <a href="http://idnc.library.illinois.edu">Illinois
 * Digital Newspaper Collections</a>.</p>
 *
 * <p>This service uses a not-officially-documented, UIUC-specific feature of
 * Veridian that UIUC requested to support harvesting. Here is the email from
 * Veridian that explains how to use it:</p>
 *
 * <blockquote>{@code
 * 1) Find the most recent time you have extracted this information. If this
 *    is your first time, it is best to set the date to 20000101. Times before
 *    1970 will not work.
 * 2) Go to http://idnc.library.illinois.edu/custom/illinois/web/script/create_recently_modified_pagelist.php?lastharvesteddate=[DATE].
 *    If this is your first time, you should go to
 *    http://idnc.library.illinois.edu/custom/illinois/web/script/create_recently_modified_pagelist.php?lastharvesteddate=20000101.
 *    Dates should be in YYYYMMDD format, but it technically supports any value
 *    that PHP's strtotime() method supports.
 * 3) If all goes well, you should get a simple HTML response saying "Success.
 *    The server is now creating the resulting XML file. Once that is finished,
 *    it will be written to
 *    http://idnc.library.illinois.edu/custom/illinois/web/modified_pageOIDs.xml"
 * 4) Wait some time. 5 minutes should be enough time in all situations, but it
 *    does seem to vary quite significantly as the script needs to check quite
 *    a few files. You know that the process is finished when the link points
 *    to a valid XML file. While the process is running, the XML file will only
 *    display the first two lines for a very long time, so your script can poll
 *    it every second without any risk of overloading the server. Note that if
 *    this is your first time, the resulting XML file will be massive (~150 MB),
 *    and would be too big for browsers like Firefox or Chrome to open. You
 *    should use wget or a similar command-line program to download the
 *    resulting XML file.
 * 5) The output of the XML file, when it is complete, will look like this (if
 *    there were no errors):
 *
 *    <xml>
 *      <info>Started at [DATE]</info>
 *      <link>[Link to a page that has been modified since the date in step 1]</link>
 *        ...
 *      <info>Ended at [DATE]</info>
 *    </xml>
 *
 *    When you next need to get the recently modified pages, you can add the
 *    value in the "Started at [DATE]" to determine which day should be used.
 *    For example, if it says "Started at Tue Sep 4 21:34:03 2018", then the
 *    next time you perform step 2, you just need to go to
 *    http://idnc.library.illinois.edu/custom/illinois/web/script/create_recently_modified_pagelist.php?lastharvesteddate=20180904.
 *    The difference between the Start and End times should say how long it
 *    took to find all of the recently modified pages.}
 * </blockquote>
 *
 * @see <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/">
 *     Veridian XML API Documentation</a>
 * @author Alex Dolski UIUC
 */
final class IDNCService implements SourceService {

    private static final class IDNCEntity implements ConcreteEntity {

        private Document doc;

        /**
         * @param xml <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent">
         *            GetPageContent</a> representation.
         */
        private static IDNCEntity fromXML(String xml) throws IOException,
                ParserConfigurationException, SAXException {
            try (Reader reader = new StringReader(xml)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(reader));
                return new IDNCEntity(doc);
            }
        }

        /**
         * @param doc <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent">
         *            GetPageContent</a> representation.
         */
        private IDNCEntity(Document doc) {
            this.doc = doc;
        }

        @Override
        public String getAccessImageURI() {
            // This is a full-sized image, likely several thousand pixels on a
            // side. GIF, JPEG, and PNG are available.
            // N.B.: width= and height= arguments are also available. The full
            // dimensions are encoded in sub-elements of the /Page/PageMetadata
            // element.
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
                fullText = Jsoup.parse(fullText).text(); // strip tags
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

        private Path harvestResultsFile;
        private XMLStreamReader xmlReader;

        private PageIterator(Path harvestResultsFile) {
            this.harvestResultsFile = harvestResultsFile;
        }

        private void close() {
            if (xmlReader != null) {
                try {
                    xmlReader.close();
                } catch (XMLStreamException e) {
                    LOGGER.error("PageIterator.close(): {}", e.getMessage());
                }
            }
        }

        @Override
        public Entity next() throws Exception {
            if (isClosed.get()) {
                close();
                throw new EndOfIterationException("Closed");
            }

            String pageURI;

            synchronized (this) {
                if (xmlReader == null) {
                    close();
                    XMLInputFactory factory = XMLInputFactory.newInstance();
                    Reader reader = Files.newBufferedReader(harvestResultsFile);
                    xmlReader = factory.createXMLStreamReader(reader);
                }

                try {
                    while (true) {
                        xmlReader.next();
                        if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT &&
                                "link".equals(xmlReader.getLocalName())) {
                            pageURI = xmlReader.getElementText();
                            break;
                        }
                    }
                } catch (NoSuchElementException e) {
                    close();
                    throw new EndOfIterationException();
                }
            }

            return fetchPage(pageURI);
        }

        /**
         * Fetches a page XML representation using the Veridian XML API's
         * {@literal GetPageContent} method.
         *
         * @see <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent">
         *     GetPageContent</a>
         */
        private Entity fetchPage(final String pageURI) throws IOException {
            // Example: http://idnc.library.illinois.edu/cgi-bin/illinois?a=d&d=CHP19370109.1.4&f=XML
            LOGGER.debug("Fetching page: {}", pageURI);

            try {
                ContentResponse response = getClient()
                        .newRequest(pageURI)
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                if (response.getStatus() == HttpStatus.OK_200) {
                    String body = response.getContentAsString();
                    return IDNCEntity.fromXML(body);
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + pageURI);
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
     * YYYYMMDD format. Used for full harvests. May be overridden by {@link
     * #lastModified} for incremental harvests.
     */
    private static final int DEFAULT_LAST_MODIFIED = 20000101;

    /**
     * After a harvest is initiated, the results will be polled at this
     * interval to check whether they're available.
     */
    private static final int RESULTS_POLL_INTERVAL_MSEC = 10000;

    /**
     * Maximum amount of time to wait for results to arrive.
     */
    private static final long MAX_POLL_WAIT_MSEC = 20 * 60 * 1000;

    private HttpClient client;

    private final AtomicBoolean isClosed           = new AtomicBoolean();
    private final AtomicBoolean isResultsAvailable = new AtomicBoolean();

    /**
     * YYYYMMDD format. If set to &gt; 0, an incremental harvest will be
     * performed and {@link #DEFAULT_LAST_MODIFIED} will be ignored.
     */
    private int lastModified;

    /**
     * Cached by {@link #sendHarvestRequest()}.
     */
    private String harvestResultsURI;

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

    private static String getHarvestScriptURI() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_IDNC_HARVEST_SCRIPT_URI");
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

    private int getLastModified() {
        if (lastModified > 0) {
            return lastModified;
        }
        return DEFAULT_LAST_MODIFIED;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * N.B.: This may take several minutes to return.
     */
    @Override
    public synchronized int numEntities() throws IOException {
        final String harvestResultsURI = sendHarvestRequest();
        waitForHarvestResults(harvestResultsURI);
        fetchHarvestResults(harvestResultsURI);
        Path resultsFile = fetchHarvestResults(harvestResultsURI);

        // Count the number of <link> elements in the harvest results.
        int count = 0;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        Reader reader = null;
        XMLStreamReader xmlReader = null;
        try {
            reader = Files.newBufferedReader(resultsFile);
            xmlReader = factory.createXMLStreamReader(reader);
            while (xmlReader.hasNext()) {
                xmlReader.next();
                if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT &&
                        "link".equals(xmlReader.getLocalName())) {
                    count++;
                }
            }
            return count;
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (xmlReader != null) {
                try {
                    xmlReader.close();
                } catch (XMLStreamException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * N.B.: This may take several minutes to return.
     */
    @Override
    public synchronized ConcurrentIterator<? extends Entity> entities()
            throws IOException {
        final String harvestResultsURI = sendHarvestRequest();
        waitForHarvestResults(harvestResultsURI);
        Path resultsFile = fetchHarvestResults(harvestResultsURI);

        return new PageIterator(resultsFile);
    }

    /**
     * @return URI of the XML harvest results. Note that the XML will be
     *         incomplete and malformed for several minutes before Veridian
     *         has fully written it.
     */
    private synchronized String sendHarvestRequest() throws IOException {
        if (harvestResultsURI == null) {
            try {
                final String uri = String.format("%s?lastharvesteddate=%d",
                        getHarvestScriptURI(), getLastModified());

                LOGGER.debug("Initiating harvest: {}", uri);

                ContentResponse response = getClient()
                        .newRequest(uri)
                        .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                        .send();
                if (response.getStatus() == 200) {
                    // The response entity is expected to look like:
                    //
                    // <html>
                    //  <head>
                    //    <title>Live Illinois Recently Modified Page Detector</title>
                    //  </head>
                    //  <body>
                    //    <p>Success. The server is now creating the resulting
                    //    XML file. Once that is finished, it will be written to
                    //    <a href="http://idnc.library.illinois.edu/custom/illinois/web/modified_pageOIDs.xml">
                    //    http://idnc.library.illinois.edu/custom/illinois/web/modified_pageOIDs.xml</a></p>
                    //  </body>
                    // </html>
                    //
                    // We need to extract that link.
                    String entity = response.getContentAsString();
                    org.jsoup.nodes.Document doc = Jsoup.parse(entity);
                    org.jsoup.nodes.Element link = doc.select("a").get(0);
                    harvestResultsURI = link.absUrl("href");
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + uri);
                }
            } catch (ExecutionException | InterruptedException |
                    TimeoutException e) {
                throw new IOException(e);
            }
        }
        return harvestResultsURI;
    }

    /**
     * Returns when results are available.
     *
     * @param uri URI returned from {@link #sendHarvestRequest()}.
     * @throws IOException if anything goes wrong or if {@link
     *         #MAX_POLL_WAIT_MSEC} is exceeded.
     */
    private void waitForHarvestResults(String uri) throws IOException {
        if (isResultsAvailable.get()) {
            return;
        }

        final Instant start = Instant.now();
        Instant now = Instant.now();

        try {
            while (true) {
                if (now.toEpochMilli() - start.toEpochMilli() > MAX_POLL_WAIT_MSEC) {
                    throw new TimeoutException("Wait time for results exceeded" +
                            (MAX_POLL_WAIT_MSEC / 1000) + " seconds");
                }

                LOGGER.debug("Polling harvest results: HEAD {}", uri);

                ContentResponse response = getClient()
                        .newRequest(uri)
                        .method(HttpMethod.HEAD)
                        .timeout(RESULTS_POLL_INTERVAL_MSEC / 1000, TimeUnit.SECONDS)
                        .send();
                if (response.getStatus() == 200) {
                    // Before results have arrived, the response entity will be
                    // a malformed XML string like:
                    //
                    // <xml>
                    //   <info>Started at Fri Sep 21 09:45:26 2018</info>
                    //
                    // We'll keep checking the length and break when it has
                    // increased.
                    long contentLength =
                            Long.parseLong(response.getHeaders().get("Content-Length"));
                    if (contentLength > 150) {
                        isResultsAvailable.set(true);
                        break;
                    }
                } else {
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for HEAD " + harvestResultsURI);
                }

                Thread.sleep(RESULTS_POLL_INTERVAL_MSEC);
                now = Instant.now();
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
    }

    /**
     * Downloads harvest results to a temporary location. Although it might be
     * possible for the {@link PageIterator} to consume them directly from the
     * socket, the iteration may take a long time (many hours) and the
     * filesystem is a stabler location.
     */
    private Path fetchHarvestResults(String harvestResultsURI)
            throws IOException {
        Path tempFile = Files.createTempFile(
                IDNCService.class.getSimpleName() + "-", ".tmp");
        Files.delete(tempFile);

        LOGGER.debug("Downloading results from {} to {}",
                harvestResultsURI, tempFile);

        final InputStreamResponseListener responseListener =
                new InputStreamResponseListener();

        try {
            getClient()
                    .newRequest(harvestResultsURI)
                    .timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send(responseListener);

            // Wait for the response headers to arrive.
            Response response = responseListener.get(
                    REQUEST_TIMEOUT, TimeUnit.SECONDS);

            if (response.getStatus() == HttpStatus.OK_200) {
                try (InputStream is =
                             new BufferedInputStream(responseListener.getInputStream())) {
                    Files.copy(is, tempFile);
                }
            } else {
                throw new IOException("Got HTTP " + response.getStatus() +
                        " for " + harvestResultsURI);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
        return tempFile;
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
