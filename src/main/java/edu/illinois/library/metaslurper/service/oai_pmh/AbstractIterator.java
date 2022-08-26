package edu.illinois.library.metaslurper.service.oai_pmh;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractIterator<T> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractIterator.class);

    final AtomicInteger numEntities = new AtomicInteger(-1);
    final Queue<T> batch = new ConcurrentLinkedQueue<>();
    ElementTransformer elementTransformer;

    private final OkHttpClient client;

    AbstractIterator(OkHttpClient client, ElementTransformer tx) {
        this.client = client;
        this.elementTransformer = tx;
    }

    Document fetchDocument(String uri) throws IOException {
        LOGGER.debug("fetchDocument(): requesting {}", uri);

        Request.Builder builder = new Request.Builder()
                .method("GET", null)
                .url(uri);
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                try (InputStream is = response.body().byteStream()) {
                    final DocumentBuilderFactory factory =
                            DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    final DocumentBuilder docBuilder = factory.newDocumentBuilder();
                    return docBuilder.parse(is);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            } else {
                throw new IOException("Received HTTP " + response.code() +
                        " for " + uri);
            }
        }
    }

}
