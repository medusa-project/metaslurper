package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Variant;
import edu.illinois.library.metaslurper.service.oai_pmh.PMHSet;

import java.util.Set;
import java.util.stream.Collectors;

final class IDEALSSet extends IDEALSEntity implements ConcreteEntity {

    private PMHSet pmhSet;

    IDEALSSet(PMHSet set) {
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
        elements.add(new Element("service", IDEALSService.PUBLIC_NAME));
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
        // N.B.: support dots in the prefix
        if (sourceID.matches("(com|col)_[\\d+.]+_\\d+")) {
            String[] parts = sourceID.split("_");
            if (parts.length == 3) {
                return String.format("http://hdl.handle.net/%s/%s",
                        parts[1], parts[2]);
            }
        }
        IDEALSService.LOGGER.warn("Unrecognized setSpec: {}", sourceID);
        return null;
    }

    @Override
    public Variant getVariant() {
        return Variant.COLLECTION;
    }

}
