package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.async.ThreadPool;
import edu.illinois.library.metaslurper.config.ConfigurationFactory;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.service.oai_pmh.Harvester;
import edu.illinois.library.metaslurper.service.oai_pmh.Listener;
import edu.illinois.library.metaslurper.service.oai_pmh.OAIPMHException;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

final class IDEALSService implements SourceService {

    private static final class IDEALSEntity implements Entity {

        private Set<Element> sourceElements;
        private Variant variant;

        IDEALSEntity(Set<Element> sourceElements, Variant variant) {
            this.sourceElements = new HashSet<>(sourceElements);
            this.variant = variant;
        }

        @Override
        public String getAccessImageURI() {
            return null;
        }

        @Override
        public Set<Element> getElements() {
            return sourceElements;
        }

        @Override
        public String getMediaType() {
            return sourceElements.stream()
                    .filter(e -> "dc:format".equals(e.getName()) &&
                            e.getValue().matches("^\\w+/([^\\s]+)"))
                    .map(Element::getValue)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String getServiceKey() {
            Configuration config = ConfigurationFactory.getConfiguration();
            return config.getString("service.source.ideals.key");
        }

        /**
         * @return String in the format {@literal {@link
         *         #getServiceKey()}-[Base16 SHA-1 hash of
         *         {@link #getSourceID()}]}
         */
        @Override
        public String getSinkID() {
            final StringBuilder builder = new StringBuilder();
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] sha1 = md.digest(getSourceID().getBytes());
                for (byte b : sha1) {
                    builder.append(String.format("%02x", b));
                }
                return getServiceKey() + "-" + builder.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return For {@link Variant#COLLECTION collections}, a string like
         *         {@literal com_2142_9462}. For {@link Variant#ITEM items}, a
         *         URI.
         */
        @Override
        public String getSourceID() {
            Stream<Element> stream = sourceElements.stream();
            switch (getVariant()) {
                case COLLECTION:
                    stream = stream.filter(e -> "setSpec".equals(e.getName()));
                    break;
                default:
                    // Items may have multiple dc:identifier elements, and even
                    // different handle URIs in different dc:identifier
                    // elements.
                    stream = stream.filter(e -> "dc:identifier".equals(e.getName()) &&
                                    e.getValue().matches("http://hdl\\.handle\\.net/\\d+/\\d+"));
                    break;
            }
            return stream.map(Element::getValue).findFirst().orElse(null);
        }

        @Override
        public String getSourceURI() {
            switch (getVariant()) {
                case COLLECTION:
                    // Transform the setSpec, like com_2142_9462, into a handle
                    // URI, like http://hdl.handle.net/2142/9462
                    String sourceID = getSourceID();
                    if (sourceID.matches("(com|col)_\\d+_\\d+")) {
                        String[] parts = sourceID.split("_");
                        if (parts.length == 3) {
                            return String.format("http://hdl.handle.net/%s/%s",
                                    parts[1], parts[2]);
                        }
                    }
                    LOGGER.warn("Unrecognized setSpec: {}", sourceID);
                    return null;
                default:
                    sourceID = getSourceID();
                    try {
                        // This should be a handle URI.
                        URI uri = new URI(sourceID);
                        return uri.toString();
                    } catch (URISyntaxException e) {
                        LOGGER.warn("Not a URI: {}", sourceID);
                        return null;
                    }
            }
        }

        @Override
        public Variant getVariant() {
            return variant;
        }

        @Override
        public String toString() {
            return getSourceID() + " / " + getSinkID();
        }

    }

    private final class CustomListener implements Listener {

        @Override
        public void onRecord(Set<Element> elements) {
            IDEALSEntity entity = new IDEALSEntity(elements, Variant.ITEM);
            resultsQueue.add(entity);
        }

        @Override
        public void onSet(Set<Element> elements) {
            IDEALSEntity entity = new IDEALSEntity(elements, Variant.COLLECTION);
            resultsQueue.add(entity);
        }

        @Override
        public void onError(OAIPMHException error) {
            // TODO: handle this
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IDEALSService.class);

    private static final String NAME = "IDEALS";

    private final Harvester harvester = new Harvester();
    private final BlockingQueue<Entity> resultsQueue =
            new LinkedBlockingQueue<>();
    private int numEntities = -1;

    IDEALSService() {
        Configuration config = ConfigurationFactory.getConfiguration();
        String endpointURI = config.getString("service.source.ideals.endpoint");
        harvester.setEndpointURI(endpointURI);
        harvester.setListener(new CustomListener());
    }

    @Override
    public void close() {
        harvester.close();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ConcurrentIterator<Entity> entities() throws IOException {
        final AtomicBoolean shouldAbort = new AtomicBoolean();

        // Harvest results into a queue in a separate thread.
        ThreadPool.getInstance().submit(() -> {
            try {
                harvester.harvest();
            } catch (IOException e) {
                shouldAbort.set(true);
                throw new UncheckedIOException(e);
            }
        });

        final int numEntities = numEntities();

        // Return an iterator that consumes the queue.
        return new ConcurrentIterator<Entity>() {
            private final AtomicInteger index = new AtomicInteger();

            @Override
            public Entity next() throws EndOfIterationException,
                    IterationException {
                if (shouldAbort.get()) {
                    throw new IterationException("Aborting prematurely. " +
                            "Something probably went wrong in a ListRecords " +
                            "or ListSets response.");
                }
                try {
                    if (index.getAndIncrement() < numEntities) {
                        return resultsQueue.take();
                    } else {
                        throw new EndOfIterationException();
                    }
                } catch (InterruptedException e) {
                    throw new UncheckedIOException(new IOException(e));
                }
            }
        };
    }

    @Override
    public int numEntities() throws IOException {
        if (numEntities < 0) {
            numEntities = harvester.getNumRecords() + harvester.getNumSets();
        }
        return numEntities;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
