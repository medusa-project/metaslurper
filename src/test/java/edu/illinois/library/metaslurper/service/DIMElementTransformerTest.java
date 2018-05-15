package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.service.oai_pmh.OAINamespaceContext;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import java.io.File;

import static org.junit.Assert.*;

public class DIMElementTransformerTest {

    private DIMElementTransformer instance;

    @Before
    public void setUp() {
        instance = new DIMElementTransformer();
    }

    @Test
    public void testTransform() throws Exception {
        final DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(new File("src/test/resources/edu/illinois/library/metaslurper/service/ListRecords-DIM.xml"));
        final XPathFactory xPathFactory = XPathFactory.newInstance();
        final XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(new OAINamespaceContext());

        XPathExpression expr = xpath.compile("//oai:record/oai:metadata/*/*");
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        Node node = nodes.item(0);
        Element e = instance.transform(node);
        assertEquals("dc:creator", e.getName());
        assertEquals("Grady, Michael", e.getValue());

        node = nodes.item(3);
        e = instance.transform(node);
        assertEquals("dc:date:accessioned", e.getName());
        assertEquals("2005-09-21T17:43:50Z", e.getValue());
    }

}