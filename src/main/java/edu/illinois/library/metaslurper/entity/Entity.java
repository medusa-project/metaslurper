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
     * @return IANA media type of the main binary represented by the item.
     *         Should be a specific type and not e.g. {@literal
     *         application/octet-stream} (unless it really is unspecified
     *         binary data). Should be {@literal null} if unknown.
     */
    String getMediaType();

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
