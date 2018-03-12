package edu.illinois.library.metaslurper.entity;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public final class Item {

    private String id;
    private URI sourceURI;

    private final Set<Element> elements = new HashSet<>();

    /**
     * Initializes an instance.
     *
     * @param id The instance's ID. It must be unique within the aggregation
     *           application, and it should be stable enough that the same
     *           resource in the source system should always receive the same
     *           ID in the aggregation system.
     * @throws IllegalArgumentException if the ID is {@literal null} or empty.
     */
    public Item(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID is null or empty");
        }
        this.id = id;
    }

    /**
     * @return The instance's unique ID.
     */
    public String getID() {
        return id;
    }

    /**
     * @return Elements ascribed to the instance. Element names must be present
     *         in the the set of available element names in the aggregation
     *         application.
     */
    public Set<Element> getElements() {
        return elements;
    }

    /**
     * @return URI of the item in the source system.
     */
    public URI getSourceURI() {
        return sourceURI;
    }

    /**
     * @param sourceURI URI of the item in the source system.
     */
    public void setSourceURI(URI sourceURI) {
        this.sourceURI = sourceURI;
    }

    @Override
    public String toString() {
        return getID();
    }

}
