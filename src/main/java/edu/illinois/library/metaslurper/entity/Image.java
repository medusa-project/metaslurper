package edu.illinois.library.metaslurper.entity;

public final class Image {

    public enum Crop {
        FULL("full"), SQUARE("square");

        private String iiifToken;

        Crop(String iiifToken) {
            this.iiifToken = iiifToken;
        }

        /**
         * @see <a href="https://iiif.io/api/image/2.1/#region">IIIF Image API
         * 2.1</a>
         */
        public String toIIIFRegion() {
            return iiifToken;
        }
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
