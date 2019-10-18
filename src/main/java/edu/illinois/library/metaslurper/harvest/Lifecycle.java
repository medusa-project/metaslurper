package edu.illinois.library.metaslurper.harvest;

public enum Lifecycle {

    /**
     * The harvest is new.
     */
    NEW(true),

    /**
     * The harvest is running.
     */
    RUNNING(true),

    /**
     * The harvest has been aborted.
     */
    ABORTED(false),

    /**
     * The harvest has completed successfully. (This does not necessarily
     * mean that all items were ingested successfully.)
     */
    SUCCEEDED(false),

    /**
     * The harvest encountered a fatal error and terminated prematurely.
     */
    FAILED(false);

    private boolean isOpen;

    Lifecycle(boolean isOpen) {
        this.isOpen = isOpen;
    }

    boolean isOpen() {
        return isOpen;
    }

}
