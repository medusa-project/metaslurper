package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

final class IDNCEntity implements ConcreteEntity {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IDNCEntity.class);

    private final Document doc;

    /**
     * @param xml <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent">
     *            GetPageContent</a> representation.
     */
    static IDNCEntity fromXML(String xml) throws IOException,
            ParserConfigurationException, SAXException {
        try (Reader reader = new StringReader(xml)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(reader));
            return new IDNCEntity(doc);
        }
    }

    /**
     * @param is <a href="https://www.veridiansoftware.com/knowledge-base/veridian-xml-api-documentation/#getpagecontent">
     *           GetPageContent</a> representation.
     */
    static IDNCEntity fromXML(InputStream is) throws IOException,
            ParserConfigurationException, SAXException {
        try (Reader reader = new InputStreamReader(is)) {
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
    public Set<Image> getAccessImages() {
        // Sample image request:
        // https://idnc.library.illinois.edu/?a=is&oid=AUE19260423.1.3&type=rawimage
        String uri = String.format(
                "%s/?a=is&oid=%s&type=rawimage",
                IDNCService.getEndpointURI(), getSourceID());
        return Set.of(new Image(uri, Image.Crop.FULL, 0, true));
    }

    @Override
    public Set<Element> getElements() {
        final Set<Element> elements = new HashSet<>();

        // service name
        elements.add(new Element("service", IDNCService.PUBLIC_NAME));

        String date = string("//DocumentMetadata/DocumentDate");
        if (!date.isEmpty()) {
            elements.add(new Element("date", date));
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
    public String getFullText() {
        String fullText = string("//PageTextHTML");
        if (!fullText.isBlank()) {
            return Jsoup.parse(fullText).text(); // strip tags
        }
        return null;
    }

    @Override
    public String getServiceKey() {
        return IDNCService.getKeyFromConfiguration();
    }

    @Override
    public String getSourceID() {
        return string("//PageMetadata/PageID");
    }

    @Override
    public String getSourceURI() {
        return IDNCService.getEndpointURI() + string("//PageViewURL");
    }

    @Override
    public String getSinkID() {
        return IDNCService.ENTITY_ID_PREFIX + getSourceID().replace(".", "_");
    }

    @Override
    public String getParentSinkID() {
        return null;
    }

    @Override
    public String getContainerName() {
        return null;
    }

    @Override
    public String getContainerSinkID() {
        return null;
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