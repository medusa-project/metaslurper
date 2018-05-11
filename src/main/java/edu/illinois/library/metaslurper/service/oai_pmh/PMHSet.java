package edu.illinois.library.metaslurper.service.oai_pmh;

public final class PMHSet extends PMHEntity {

    private String spec, name;

    PMHSet() {}

    public String getName() {
        return name;
    }

    public String getSpec() {
        return spec;
    }

    void setName(String name) {
        this.name = name;
    }

    void setSpec(String spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        return getSpec() + "";
    }

}
