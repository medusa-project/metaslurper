package edu.illinois.library.metaslurper.harvest;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe class for tracking the status of a harvest.
 *
 * @author Alex Dolski UIUC
 */
public class Status {

    private Lifecycle lifecycle              = Lifecycle.NEW;
    private final AtomicInteger numSucceeded = new AtomicInteger();
    private final AtomicInteger numFailed    = new AtomicInteger();
    private final Queue<String> messages     = new ConcurrentLinkedQueue<>();

    public synchronized Lifecycle getLifecycle() {
        return lifecycle;
    }

    public int addAndGetNumFailed(int delta) {
        return numFailed.addAndGet(delta);
    }

    public Queue<String> getMessages() {
        return messages;
    }

    public int getNumFailed() {
        return numFailed.get();
    }

    public int getNumSucceeded() {
        return numSucceeded.get();
    }

    void incrementAndGetNumFailed() {
        numFailed.incrementAndGet();
    }

    void incrementAndGetNumSucceeded() {
        numSucceeded.incrementAndGet();
    }

    synchronized void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%d succeeded, %d failed)",
                getClass().getSimpleName(), getLifecycle(),
                getNumSucceeded(), getNumFailed());
    }

}
