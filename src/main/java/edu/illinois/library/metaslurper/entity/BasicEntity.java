package edu.illinois.library.metaslurper.entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Ready-made bean-style {@link Entity} implementation.
 */
public class BasicEntity implements Entity {

    private String sourceID, sinkID, serviceKey, accessImageURI, sourceURI;
    private Variant variant = Variant.UNKNOWN;

    private final Set<Element> elements = new HashSet<>();

    @Override
    public String getAccessImageURI() {
        return accessImageURI;
    }

    @Override
    public Set<Element> getElements() {
        return elements;
    }

    @Override
    public String getServiceKey() {
        return serviceKey;
    }

    @Override
    public String getSinkID() {
        return sinkID;
    }

    @Override
    public String getSourceID() {
        return sourceID;
    }

    @Override
    public String getSourceURI() {
        return sourceURI;
    }

    @Override
    public Variant getVariant() {
        return variant;
    }

    /**
     * @throws IllegalArgumentException if the argument is {@literal null} or
     *         empty.
     */
    public void setAccessImageURI(String imageURI) {
        if (imageURI == null || imageURI.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.accessImageURI = imageURI;
    }

    /**
     * @throws IllegalArgumentException if the argument is {@literal null} or
     *         empty.
     */
    public void setServiceKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.serviceKey = key;
    }

    /**
     * @throws IllegalArgumentException if the argument is {@literal null} or
     *         empty.
     */
    public void setSinkID(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.sinkID = id;
    }

    /**
     * @throws IllegalArgumentException if the argument is {@literal null} or
     *         empty.
     */
    public void setSourceID(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.sourceID = id;
    }

    /**
     * @throws IllegalArgumentException if the argument is {@literal null} or
     *         empty.
     */
    public void setSourceURI(String sourceURI) {
        if (sourceURI == null || sourceURI.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.sourceURI = sourceURI;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    @Override
    public String toString() {
        return getServiceKey() + " " + getSinkID();
    }

}
