package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.service.oai_pmh.Harvester;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHRecord;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Harvests metadata in DIM format from the OAI-PMH endpoint of IDEALS.
 */
final class IDEALSService implements SourceService {

    private static abstract class IDEALSEntity {

        public String getAccessImageURI() {
            return null;
        }

        public String getServiceKey() {
            return getKeyFromConfiguration();
        }

        public String getSinkID() {
            return getServiceKey() + "-" +
                    getSourceID().replaceAll("[^A-Za-z\\d]", "_");
        }

        public abstract String getSourceID();

        @Override
        public String toString() {
            return getSourceID() + " / " + getSinkID();
        }

    }

    private static final class IDEALSRecord extends IDEALSEntity
            implements Entity {

        private PMHRecord pmhRecord;

        private IDEALSRecord(PMHRecord record) {
            this.pmhRecord = record;
        }

        @Override
        public Set<Element> getElements() {
            return pmhRecord.getElements();
        }

        @Override
        public String getMediaType() {
            return pmhRecord.getElements()
                    .stream()
                    .filter(e -> "dc:format:mimetype".equals(e.getName()) &&
                            e.getValue().matches("^\\w+/([^\\s]+)"))
                    .map(Element::getValue)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * @return Handle URI.
         */
        @Override
        public String getSourceID() {
            return pmhRecord.getElements()
                    .stream()
                    .filter(e -> "dc:identifier:uri".equals(e.getName()) &&
                            e.getValue().matches("http://hdl\\.handle\\.net/\\d+/\\d+"))
                    .map(Element::getValue)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String getSourceURI() {
            String sourceID = getSourceID();
            try {
                // This should be a handle URI.
                URI uri = new URI(sourceID);
                return uri.toString();
            } catch (URISyntaxException e) {
                LOGGER.warn("Not a URI: {}", sourceID);
                return null;
            }
        }

        @Override
        public Variant getVariant() {
            return Variant.ITEM;
        }

    }

    private static final class IDEALSSet extends IDEALSEntity
            implements Entity {

        private PMHSet pmhSet;

        private IDEALSSet(PMHSet set) {
            this.pmhSet = set;
        }

        @Override
        public Set<Element> getElements() {
            Set<Element> elements = pmhSet.getElements()
                    .stream()
                    .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                    .collect(Collectors.toSet());
            elements.add(new Element("setName", pmhSet.getName()));
            elements.add(new Element("setSpec", pmhSet.getSpec()));
            return elements;
        }

        @Override
        public String getSourceID() {
            return pmhSet.getSpec();
        }

        @Override
        public String getSourceURI() {
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
        }

        @Override
        public Variant getVariant() {
            return Variant.COLLECTION;
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IDEALSService.class);

    private static final String NAME = "IDEALS";

    private final Harvester harvester = new Harvester();
    private int numEntities = -1;

    private static String getKeyFromConfiguration() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_IDEALS_KEY");
    }

    IDEALSService() {
        Configuration config = Configuration.getInstance();
        String endpointURI = config.getString("SERVICE_SOURCE_IDEALS_ENDPOINT");
        harvester.setEndpointURI(endpointURI);
        harvester.setMetadataPrefix("dim");
    }

    @Override
    public void close() {
        harvester.close();
    }

    @Override
    public String getKey() {
        return getKeyFromConfiguration();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ConcurrentIterator<Entity> entities() throws IOException {
        final ConcurrentIterator<PMHRecord> records =
                harvester.records(new DIMElementTransformer());
        final ConcurrentIterator<PMHSet> sets = harvester.sets();

        return () -> {
            try {
                return new IDEALSSet(sets.next());
            } catch (EndOfIterationException e) {
                return new IDEALSRecord(records.next());
            }
        };
    }

    @Override
    public int numEntities() throws IOException {
        if (numEntities < 0) {
            numEntities = harvester.numRecords() + harvester.numSets();
        }
        return numEntities;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
