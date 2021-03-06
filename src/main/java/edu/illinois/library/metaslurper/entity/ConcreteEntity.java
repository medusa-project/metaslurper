package edu.illinois.library.metaslurper.entity;

import java.util.Collections;
import java.util.Set;

/**
 * Represents an existing {@link Entity}.
 *
 * @see PlaceholderEntity
 * @author Alex Dolski UIUC
 */
public interface ConcreteEntity extends Entity {

    /**
     * @see #getAccessImages()
     */
    short MIN_ACCESS_IMAGE_POWER = 6;

    /**
     * @see #getAccessImages()
     */
    short MAX_ACCESS_IMAGE_POWER = 12;

    /**
     * <p>This default implementation returns an empty set. It should be
     * overridden by sources that can provide access images.</p>
     *
     * <p>N.B.: The images' {@link Image#getSize() sizes} are powers of 2
     * in the range of 2^{@link #MIN_ACCESS_IMAGE_POWER}-
     * 2^{@link #MAX_ACCESS_IMAGE_POWER}. URIs should use the HTTPS scheme
     * if possible. Both {@link Image#getCrop() full and square crops} should
     * be provided, if possible.</p>
     */
    default Set<Image> getAccessImages() {
        return Collections.emptySet();
    }

    /**
     * @return Elements ascribed to the instance. Element names are arbitrary.
     */
    Set<Element> getElements();

    /**
     * @return Full text associated with the entity.
     */
    default String getFullText() {
        return null;
    }

    /**
     * This default implementation returns {@code null}.
     *
     * @return IANA media type of the main binary represented by the item.
     *         Should be a specific type and not e.g. {@code
     *         application/octet-stream} (unless it really is unspecified
     *         binary data), or {@code null} if unknown.
     */
    default String getMediaType() {
        return null;
    }

    /**
     * @return Variant of the entity.
     */
    Variant getVariant();

}
