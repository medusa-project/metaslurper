package edu.illinois.library.metaslurper.entity;

import java.util.Set;

public interface Entity {

    /**
     * This default implementation returns {@literal null}.
     *
     * @return URI of a high-quality access image. May be {@literal null}.
     */
    default String getAccessImageURI() {
        return null;
    }

    /**
     * @return Elements ascribed to the instance. Element names are arbitrary.
     */
    Set<Element> getElements();

    /**
     * This default implementation returns {@literal null}.
     *
     * @return IANA media type of the main binary represented by the item.
     *         Should be a specific type and not e.g. {@literal
     *         application/octet-stream} (unless it really is unspecified
     *         binary data). Should be {@literal null} if unknown.
     */
    default String getMediaType() {
        return null;
    }

    /**
     * @return Source service key.
     */
    String getServiceKey();

    /**
     * N.B.: Some sink services may have problems with URI-illegal characters
     * in the sink ID.
     *
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
