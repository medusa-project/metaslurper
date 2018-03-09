package edu.illinois.library.metaslurper.slurp;

import java.time.Duration;

public final class SlurpResult {

    private int numSucceeded, numFailed;
    private Duration duration;

    SlurpResult(int numSucceeded, int numFailed, Duration duration) {
        this.numSucceeded = numSucceeded;
        this.numFailed = numFailed;
        this.duration = duration;
    }

    void add(SlurpResult result) {
        this.numSucceeded += result.getNumSucceeded();
        this.numFailed += result.getNumFailed();
        this.duration = result.getDuration().plus(this.duration);
    }

    public Duration getDuration() {
        return duration;
    }

    public int getNumFailed() {
        return numFailed;
    }

    public int getNumSucceeded() {
        return numSucceeded;
    }

    @Override
    public String toString() {
        return String.format("Duration: %s\n" +
                "Succeeded: %d\n"+
                "Failed: %d",
                duration, numSucceeded, numFailed);
    }
}
