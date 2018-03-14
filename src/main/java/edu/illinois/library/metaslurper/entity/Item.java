package edu.illinois.library.metaslurper.entity;

import java.net.URI;
import java.util.Set;

public interface Item {

    /**
     * @return The instance's ID within the sink service.
     */
    String getID();

    /**
     * @return Elements ascribed to the instance. Element names are arbitrary.
     */
    Set<Element> getElements();

    /**
     * @return Source service key.
     */
    String getServiceKey();

    /**
     * @return URI of the item in the source system.
     */
    URI getSourceURI();

}
