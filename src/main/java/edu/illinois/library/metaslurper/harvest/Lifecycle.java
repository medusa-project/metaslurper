package edu.illinois.library.metaslurper.harvest;

public enum Lifecycle {

    /**
     * The harvest is new.
     */
    NEW,

    /**
     * The harvest is running.
     */
    RUNNING,

    /**
     * The harvest has been aborted.
     */
    ABORTED,

    /**
     * The harvest has completed successfully. (This does not necessarily
     * mean that all items were ingested successfully.)
     */
    SUCCEEDED,

    /**
     * The harvest encountered a fatal error and terminated prematurely.
     */
    FAILED

}
