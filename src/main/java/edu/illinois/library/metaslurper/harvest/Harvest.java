package edu.illinois.library.metaslurper.harvest;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe class for tracking the status of a harvest.
 *
 * @author Alex Dolski UIUC
 */
public final class Harvest {

    private static final int MAX_MESSAGES    = 100;

    private Lifecycle lifecycle              = Lifecycle.NEW;
    private int numEntities                  = 0;
    private final AtomicInteger index        = new AtomicInteger();
    private final AtomicInteger numSucceeded = new AtomicInteger();
    private final AtomicInteger numFailed    = new AtomicInteger();
    private final Queue<String> messages     = new ConcurrentLinkedQueue<>();

    /**
     * Call to cancel a harvest before all entities have been harvested.
     */
    synchronized void abort() {
        if (lifecycle.isOpen()) {
            // Set the failure count to the number of items remaining.
            final int delta = numEntities - index.get();
            if (delta > 0) {
                numFailed.addAndGet(delta);
                addMessage("Harvest aborted with " + delta + " items left.");
            }
        }
        setLifecycle(Lifecycle.ABORTED);
    }

    public void addMessage(String message) {
        messages.add(message);
        if (messages.size() > MAX_MESSAGES) {
            messages.poll();
        }
    }

    /**
     * Call to end a harvest when there are no more entities left to harvest
     * but the number of harvested entities is less than the declared entity
     * count.
     */
    synchronized void endPrematurely() {
        if (lifecycle.isOpen()) {
            final int delta = numEntities - index.get();
            if (delta > 0) {
                numFailed.addAndGet(delta);
                addMessage("Added " + delta + " to the failure count " +
                        "due to a discrepancy between the number of items " +
                        "reported present in the service (" + numEntities +
                        ") and the number found (" + (getNumSucceeded() +
                        getNumFailed()) + ").");
            }
        }
        setLifecycle(Lifecycle.SUCCEEDED);
    }

    public synchronized Lifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * N.B.: Do not mutate the return value; use {@link #addMessage(String)}
     * instead.
     *
     * @return All messages in the queue.
     */
    public Queue<String> getMessages() {
        return messages;
    }

    public int getAndIncrementIndex() {
        return index.getAndIncrement();
    }

    /**
     * @return The last index that any thread has reported attempting to
     *         harvest.
     */
    public int getIndex() {
        return index.get();
    }

    public synchronized int getNumEntities() {
        return numEntities;
    }

    public int getNumFailed() {
        return numFailed.get();
    }

    public int getNumSucceeded() {
        return numSucceeded.get();
    }

    void incrementNumFailed() {
        numFailed.incrementAndGet();
    }

    void incrementNumSucceeded() {
        numSucceeded.incrementAndGet();
    }

    public int numMessages() {
        return messages.size();
    }

    synchronized void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * @param numEntities Number of entities reported to be available in the
     *                    source service.
     */
    synchronized void setNumEntities(int numEntities) {
        this.numEntities = numEntities;
    }

    @Override
    public String toString() {
        return String.format("%s: %s [%d total] [%d succeeded] [%d failed]",
                getClass().getSimpleName(),
                getLifecycle(),
                getNumEntities(),
                getNumSucceeded(),
                getNumFailed());
    }

}
