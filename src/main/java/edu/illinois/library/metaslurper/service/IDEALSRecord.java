package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHRecord;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

final class IDEALSRecord extends IDEALSEntity implements ConcreteEntity {

    private PMHRecord pmhRecord;

    IDEALSRecord(PMHRecord record) {
        this.pmhRecord = record;
    }

    @Override
    public Set<Element> getElements() {
        Set<Element> elements = pmhRecord.getElements();
        elements.add(new Element("service", IDEALSService.PUBLIC_NAME));
        return elements;
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
                        e.getValue().matches("(http|https)://hdl\\.handle\\.net/\\d+/\\d+"))
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
            IDEALSService.LOGGER.warn("Not a URI: {}", sourceID);
            return null;
        }
    }

    @Override
    public Variant getVariant() {
        return Variant.PAPER;
    }

}
