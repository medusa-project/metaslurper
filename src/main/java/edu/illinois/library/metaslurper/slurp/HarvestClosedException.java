package edu.illinois.library.metaslurper.slurp;

import java.io.IOException;

public class HarvestClosedException extends IOException {

    public HarvestClosedException() {
        super();
    }

    public HarvestClosedException(String message) {
        super(message);
    }

}
