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
     * @return Source service key.
     */
    String getServiceKey();

    /**
     * @return The instance's ID within the sink service.
     */
    String getSinkID();

    /**
     * @return Identifier of the entity in the source system.
     */
    String getSourceID();

    /**
     * @return URI of the item in the source system.
     */
    String getSourceURI();

    /**
     * @return Variant of the entity.
     */
    Variant getVariant();

}
