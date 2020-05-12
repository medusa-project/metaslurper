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
    private int maxNumEntities               = -1;
    private final AtomicInteger numSucceeded = new AtomicInteger();
    private final AtomicInteger numFailed    = new AtomicInteger();
    private final Queue<String> messages     = new ConcurrentLinkedQueue<>();

    /**
     * Cancels a harvest before all entities have been harvested.
     */
    synchronized void abort() {
        if (lifecycle.isOpen()) {
            final int canonicalNumEntities = getCanonicalNumEntities();
            // Set the failure count to the number of items remaining.
            final int delta = canonicalNumEntities - numSucceeded.get() -
                    numFailed.get();
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
     * Ends the harvest.
     */
    synchronized void end() {
        if (lifecycle.isOpen()) {
            final int canonicalNumEntities = getCanonicalNumEntities();
            int numSucceededInt            = numSucceeded.get();
            int numFailedInt               = numFailed.get();
            final int delta                = canonicalNumEntities -
                    numSucceededInt - numFailedInt;
            if (delta > 0) {
                numFailed.addAndGet(delta);
                addMessage("Added " + delta + " to the failure count " +
                        "due to a discrepancy between the number of items " +
                        "reported present in the service (" +
                        canonicalNumEntities + ") and the number found (" +
                        (numSucceededInt + numFailedInt) + ").");
            }
            setLifecycle(Lifecycle.SUCCEEDED);
        }
    }

    /**
     * Must only be called after {@link #setNumEntities(int)} and {@link
     * #setMaxNumEntities(int)}.
     *
     * @return The number of entities to be harvested. May be {@code -1} to
     *         indicate an unknown number.
     * @see #getNumEntities()
     */
    int getCanonicalNumEntities() {
        int tmp = numEntities;
        if (maxNumEntities > 0) {
            tmp = Math.min(numEntities, maxNumEntities);
        }
        return tmp;
    }

    public synchronized Lifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * @return The maximum number of entities to harvest.
     */
    public int getMaxNumEntities() {
        return maxNumEntities;
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

    /**
     * @return The number of entities available in the source service, or
     *         {@code -1} to indicate "unknown." (This may not be the same as
     *         the number of entities to be harvested.)
     * @see #getCanonicalNumEntities()
     */
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

    /**
     * @return Whether the instance is available for use.
     */
    boolean isOpen() {
        // N.B. A condition for "no max and numFailed + numSucceeded <
        // numEntities" is not included because the number of items reported
        // present in a source service may be smaller than the number of items
        // actually present.
        return getLifecycle().isOpen() &&
                ((getMaxNumEntities() > 0 &&
                        getNumFailed() + getNumSucceeded() < getMaxNumEntities()) ||
                getMaxNumEntities() < 1);
    }

    public int numMessages() {
        return messages.size();
    }

    synchronized void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * @param numEntities The maximum number of entities to harvest.
     */
    public void setMaxNumEntities(int numEntities) {
        this.maxNumEntities = numEntities;
    }

    /**
     * @param numEntities Number of entities reported to be available in the
     *                    source service. Supply {@code -1} to indicate
     *                    "unknown."
     */
    synchronized void setNumEntities(int numEntities) {
        this.numEntities = numEntities;
    }

    @Override
    public String toString() {
        return String.format("%s: %s [%s total] [%d succeeded] [%d failed]",
                getClass().getSimpleName(),
                getLifecycle(),
                (getNumEntities() > -1) ? getNumEntities() : "unknown",
                getNumSucceeded(),
                getNumFailed());
    }

}
