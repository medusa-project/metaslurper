package edu.illinois.library.metaslurper.service.oai_pmh;

public final class PMHRecord extends PMHEntity {

    private String identifier, datestamp, setSpec;

    PMHRecord() {}

    public String getDatestamp() {
        return datestamp;

    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSetSpec() {
        return setSpec;
    }

    void setDatestamp(String datestamp) {
        this.datestamp = datestamp;
    }

    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    void setSetSpec(String setSpec) {
        this.setSpec = setSpec;
    }

    @Override
    public String toString() {
        return getIdentifier() + "";
    }
}
