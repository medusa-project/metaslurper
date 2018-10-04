package edu.illinois.library.metaslurper.service;

abstract class IDEALSEntity {

    public String getServiceKey() {
        return IDEALSService.getKeyFromConfiguration();
    }

    public String getSinkID() {
        return getServiceKey() + "-" +
                getSourceID().replaceAll("[^A-Za-z\\d]", "_");
    }

    public abstract String getSourceID();

    @Override
    public String toString() {
        return getSourceID() + " / " + getSinkID();
    }

}
