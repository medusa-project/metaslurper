package edu.illinois.library.metaslurper.service;

abstract class IDEALSEntity {

    public String getServiceKey() {
        return IDEALSService.getKeyFromConfiguration();
    }

    public abstract String getSourceID();

    public String getSinkID() {
        return getServiceKey() + "-" +
                getSourceID().replaceAll("[^A-Za-z\\d]", "_");
    }

    public String getParentSinkID() {
        return null;
    }

    public String getContainerSinkID() {
        return null;
    }

    @Override
    public String toString() {
        return getSourceID() + " / " + getSinkID();
    }

}
