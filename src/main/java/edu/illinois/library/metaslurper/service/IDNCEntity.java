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
        final Set<Image> images = new HashSet<>();

        // The way to get images from Veridian is through its image
        // server, imageserver.pl. This endpoint supports the following
        // arguments:
        //
        // oid=[identifier]
        // color=[all|?]
        // ext=[jpg|gif|png]
        // crop=x,y,w,h
        // width=w
        // height=h
        //
        // It can also deliver unmodified raw images by supplying
        // getrawimage=true.
        //
        // Examples:
        // http://idnc.library.illinois.edu/cgi-bin/imageserver.pl?oid=TAG19220928.1.1&crop=&color=all&ext=jpg&width=200
        // http://idnc.library.illinois.edu/cgi-bin/imageserver.pl?oid=TAG19220928.1.1&getrawimage=true

        // master image
        String uri = String.format(
                "%s/cgi-bin/imageserver.pl?oid=%s&getrawimage=true",
                IDNCService.getEndpointURI(), getSourceID());
        images.add(new Image(uri, Image.Crop.FULL, 0, true));

        final String widthStr = string("//PageMetadata/PageImageWidth");
        final String heightStr = string("//PageMetadata/PageImageHeight");

        if (!widthStr.isEmpty() && !heightStr.isEmpty()) {
            final int fullWidth = Integer.parseInt(widthStr);
            final int fullHeight = Integer.parseInt(heightStr);

            for (int exp = ConcreteEntity.MIN_ACCESS_IMAGE_POWER;
                 exp <= ConcreteEntity.MAX_ACCESS_IMAGE_POWER;
                 exp++) {
                final int size = (int) Math.pow(2, exp);
                if (size < fullWidth && size < fullHeight) {
                    // full crop
                    uri = String.format(
                            "%s/cgi-bin/imageserver.pl?oid=%s&color=all&ext=jpg&width=%d&height=%d",
                            IDNCService.getEndpointURI(), getSourceID(),
                            size, size);
                    images.add(new Image(uri, Image.Crop.FULL, size, false));

                    // square crop
                    int cropSize = Math.min(fullWidth, fullHeight);
                    int cropX    = (int) Math.round((fullWidth - cropSize) / 2.0);
                    int cropY    = (int) Math.round((fullHeight - cropSize) / 2.0);
                    uri = String.format(
                            "%s/cgi-bin/imageserver.pl?oid=%s&color=all&ext=jpg&crop=%d,%d,%d,%d&width=%d&height=%d",
                            IDNCService.getEndpointURI(), getSourceID(),
                            cropX, cropY, cropSize, cropSize, size, size);
                    images.add(new Image(uri, Image.Crop.SQUARE, size, false));
                }
            }
        }
        return images;
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