package edu.illinois.library.metaslurper.service.oai_pmh;

import java.io.IOException;

public class OAIPMHException extends IOException {

    private String code;

    OAIPMHException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
