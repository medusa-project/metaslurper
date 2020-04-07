package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.service.ConcurrentIterator;
import edu.illinois.library.metaslurper.service.EndOfIterationException;
import okhttp3.OkHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

class RecordIterator<T> extends AbstractIterator<T>
        implements ConcurrentIterator<T> {

    private final AtomicInteger index = new AtomicInteger();
    private String endpointURI, resumptionToken, metadataPrefix;
    private Instant from, until;

    RecordIterator(OkHttpClient client,
                   String endpointURI,
                   String metadataPrefix,
                   Instant from,
                   Instant until,
                   int numRecords,
                   ElementTransformer tx) {
        super(client, tx);
        this.endpointURI = endpointURI;
        this.metadataPrefix = metadataPrefix;
        this.from = from;
        this.until = until;
        numEntities.set(numRecords);
    }

    @Override
    public T next() throws EndOfIterationException, IOException {
        if (numEntities.get() >= 0 &&
                index.incrementAndGet() >= numEntities.get()) {
            throw new EndOfIterationException();
        }

        // If the queue is empty, fetch the next batch.
        synchronized (this) {
            if (batch.peek() == null) {
                resumptionToken = fetchBatch(resumptionToken, batch);
            }
        }

        return batch.remove();
    }

    /**
     * @param resumptionToken Current resumption token.
     * @param batch           Queue to add results to.
     * @return                Next resumption token.
     */
    private String fetchBatch(String resumptionToken,
                              Queue<T> batch) throws IOException {
        String uri;
        if (resumptionToken != null && !resumptionToken.isEmpty()) {
            uri = String.format("%s?verb=ListRecords&resumptionToken=%s",
                    endpointURI, resumptionToken);
        } else {
            uri = String.format("%s?verb=ListRecords&metadataPrefix=%s",
                    endpointURI, metadataPrefix);
            if (from != null) {
                uri += "&from=" + from;
            }
            if (until != null) {
                uri += "&until=" + until;
            }
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
                    Element e = elementTransformer.transform(mdnode);
                    if (e != null) {
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