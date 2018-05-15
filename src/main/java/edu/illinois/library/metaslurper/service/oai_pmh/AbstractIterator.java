package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.service.EndOfIterationException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.illinois.library.metaslurper.service.oai_pmh.Harvester.REQUEST_TIMEOUT_SECONDS;

abstract class AbstractIterator<T> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractIterator.class);

    final AtomicInteger numEntities = new AtomicInteger(-1);
    final Queue<T> batch = new ConcurrentLinkedQueue<>();
    ElementTransformer elementTransformer;

    private HttpClient client;
    private final AtomicInteger index = new AtomicInteger();
    private String resumptionToken;

    AbstractIterator(HttpClient client, ElementTransformer tx) {
        this.client = client;
        this.elementTransformer = tx;
    }

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

        client.newRequest(uri)
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

    public T next() throws EndOfIterationException, IOException {
        if (numEntities.get() >= 0 && index.incrementAndGet() >= numEntities.get()) {
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

}
