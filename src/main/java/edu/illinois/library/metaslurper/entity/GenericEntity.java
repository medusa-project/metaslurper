package edu.illinois.library.metaslurper.entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Ready-made bean-style implementation.
 */
public class GenericEntity implements ConcreteEntity {

    private String containerName, containerSinkID, fullText, mediaType,
            sourceID, sinkID, serviceKey, sourceURI;
    private Variant variant = Variant.UNKNOWN;

    private final Set<Image> accessImages = new HashSet<>();
    private final Set<Element> elements = new HashSet<>();

    @Override
    public Set<Image> getAccessImages() {
        return accessImages;
    }

    @Override
    public String getContainerName() {
        return containerName;
    }

    @Override
    public String getContainerSinkID() {
        return containerSinkID;
    }

    @Override
    public Set<Element> getElements() {
        return elements;
    }

    @Override
    public String getFullText() {
        return fullText;
    }

    @Override
    public String getMediaType() {
        return mediaType;
    }

    @Override
    public String getServiceKey() {
        return serviceKey;
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
    public String getSinkID() {
        return sinkID;
    }

    @Override
    public String getParentSinkID() {
        return null;
    }

    @Override
    public Variant getVariant() {
        return variant;
    }

    public void addAccessImage(Image image) {
        this.accessImages.add(image);
    }

    public void setContainerSinkID(String id) {
        this.containerSinkID = id;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
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
