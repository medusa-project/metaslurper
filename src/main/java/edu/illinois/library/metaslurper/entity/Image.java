package edu.illinois.library.metaslurper.entity;

public final class Image {

    public enum Crop {
        FULL, SQUARE
    }

    private Crop crop = Crop.FULL;
    private int size;
    private String uri;

    public Image(String uri, int size, Crop crop) {
        this.uri = uri;
        this.size = size;
        this.crop = crop;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Image) {
            Image other = (Image) obj;
            return getURI().equals(other.getURI());
        }
        return super.equals(obj);
    }

    public Crop getCrop() {
        return crop;
    }

    public int getSize() {
        return size;
    }

    public String getURI() {
        return uri;
    }

    @Override
    public int hashCode() {
        return ("" + getURI()).hashCode();
    }

    @Override
    public String toString() {
        return getURI();
    }

}
