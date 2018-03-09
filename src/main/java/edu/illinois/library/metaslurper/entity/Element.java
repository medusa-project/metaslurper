package edu.illinois.library.metaslurper.entity;

/**
 * Key-value metadata element.
 */
public final class Element {

    private String name, value;

    public Element(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Element) {
            Element other = (Element) obj;
            return toString().equals(other.toString());
        }
        return super.equals(obj);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return getName() + ": " + getValue();
    }

}
