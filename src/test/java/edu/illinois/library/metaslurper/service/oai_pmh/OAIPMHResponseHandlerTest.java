package edu.illinois.library.metaslurper.service.oai_pmh;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class OAIPMHResponseHandlerTest {

    OAIPMHResponseHandler instance;

    abstract Path getErrorResponse();
    abstract Path getValidResponse();

    void readValidResponse() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(instance);

        try (InputStream is = Files.newInputStream(getValidResponse())) {
            reader.parse(new InputSource(is));
        }
    }

    void readErrorResponse() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(instance);

        try (InputStream is = Files.newInputStream(getErrorResponse())) {
            reader.parse(new InputSource(is));
        }
    }

}
