package edu.illinois.library.metaslurper.entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Ready-made {@link Item} implementation.
 */
public class BasicItem implements Item {

    private String id, serviceKey, accessImageURI, sourceURI;

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
    public String getID() {
        return id;
    }

    @Override
    public String getServiceKey() {
        return serviceKey;
    }

    @Override
    public String getSourceURI() {
        return sourceURI;
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
    public void setID(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.id = id;
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
    public void setSourceURI(String sourceURI) {
        if (sourceURI == null || sourceURI.isEmpty()) {
            throw new IllegalArgumentException("Argument is null or empty");
        }
        this.sourceURI = sourceURI;
    }

    @Override
    public String toString() {
        return getServiceKey() + " " + getID();
    }

}
