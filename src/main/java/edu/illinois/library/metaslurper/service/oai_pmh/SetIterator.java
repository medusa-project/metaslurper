package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import org.eclipse.jetty.client.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Queue;

class SetIterator<T> extends AbstractIterator<T>
        implements ConcurrentIterator<T> {

    private String endpointURI;

    SetIterator(HttpClient client,
                String endpointURI,
                int numSets,
                ElementTransformer tx) {
        super(client, tx);
        this.endpointURI = endpointURI;
        numEntities.set(numSets);
    }

    @Override
    String fetchBatch(String resumptionToken,
                      Queue<T> batch) throws IOException {
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
                    Element e = elementTransformer.transform(mdnode);
                    if (e != null) {
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
