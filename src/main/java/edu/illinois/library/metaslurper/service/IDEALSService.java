package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.config.Configuration;
import edu.illinois.library.metaslurper.entity.Entity;
import edu.illinois.library.metaslurper.service.oai_pmh.Harvester;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHRecord;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

/**
 * Harvests metadata from the OAI-PMH endpoint of
 * <a href="https://www.ideals.illinois.edu">IDEALS</a>. The {@code native}
 * metadata format is used.
 */
final class IDEALSService implements SourceService {

    static final Logger LOGGER = LoggerFactory.getLogger(IDEALSService.class);

    static final String PUBLIC_NAME = "Scholarship";

    private static final String PRIVATE_NAME = "IDEALS";

    private final Harvester harvester = new Harvester();
    private int numEntities = -1;

    static String getKeyFromConfiguration() {
        Configuration config = Configuration.getInstance();
        return config.getString("SERVICE_SOURCE_IDEALS_KEY");
    }

    IDEALSService() {
        Configuration config = Configuration.getInstance();
        String endpointURI = config.getString("SERVICE_SOURCE_IDEALS_ENDPOINT");
        harvester.setEndpointURI(endpointURI);
        harvester.setMetadataPrefix("native");
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
        return PRIVATE_NAME;
    }

    @Override
    public ConcurrentIterator<? extends Entity> entities() throws IOException {
        final ConcurrentIterator<PMHRecord> records =
                harvester.records(new NativeElementTransformer());
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
    public void setLastModified(Instant lastModified) {
        harvester.setFrom(lastModified);
        numEntities = -1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
