package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;

import java.util.HashSet;
import java.util.Set;

abstract class PMHEntity {

    private final Set<Element> elements = new HashSet<>();

    public Set<Element> getElements() {
        return elements;
    }

}
