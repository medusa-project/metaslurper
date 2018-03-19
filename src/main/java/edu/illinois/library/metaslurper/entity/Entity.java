package edu.illinois.library.metaslurper.entity;

import java.util.Set;

public interface Entity {

    /**
     * @return URI of a high-quality access image. May be {@literal null}.
     */
    String getAccessImageURI();

    /**
     * @return Elements ascribed to the instance. Element names are arbitrary.
     */
    Set<Element> getElements();

    /**
     * @return The instance's ID within the sink service.
     */
    String getID();

    /**
     * @return Source service key.
     */
    String getServiceKey();

    /**
     * @return URI of the item in the source system.
     */
    String getSourceURI();

    /**
     * @return Type of the entity.
     */
    Type getType();

}
