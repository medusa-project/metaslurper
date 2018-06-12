package edu.illinois.library.metaslurper.service;

import java.io.IOException;

class HarvestClosedException extends IOException {

    HarvestClosedException(String message) {
        super(message);
    }

}
