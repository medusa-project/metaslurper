package edu.illinois.library.metaslurper.entity;

import java.util.Set;

/**
 * Represents an existing {@link Entity}.
 *
 * @author Alex Dolski UIUC
 */
public interface ConcreteEntity extends Entity {

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
     * @return Variant of the entity.
     */
    Variant getVariant();

}
