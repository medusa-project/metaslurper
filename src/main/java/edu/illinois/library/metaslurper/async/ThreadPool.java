package edu.illinois.library.metaslurper.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Global application thread pool Singleton.
 */
public final class ThreadPool {

    public enum Priority {
        LOW, NORMAL, HIGH
    }

    private static class CustomThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    }

    private static ThreadPool instance;

    private boolean isShutdown = false;

    private final ExecutorService lowPriorityPool =
            Executors.newCachedThreadPool(new CustomThreadFactory());
    private final ExecutorService normalPriorityPool =
            Executors.newCachedThreadPool(new CustomThreadFactory());
    private final ExecutorService highPriorityPool =
            Executors.newCachedThreadPool(new CustomThreadFactory());

    /**
     * @return Shared {@link ThreadPool} instance.
     */
    public static synchronized ThreadPool getInstance() {
        if (instance == null || instance.isShutdown()) {
            instance = new ThreadPool();
        }
        return instance;
    }

    /**
     * For testing.
     */
    static synchronized void clearInstance() {
        instance.shutdown();
        instance = null;
    }

    private ThreadPool() {
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        lowPriorityPool.shutdownNow();
        normalPriorityPool.shutdownNow();
        highPriorityPool.shutdownNow();
        isShutdown = true;
    }

    public Future<?> submit(Callable<?> task) {
        return submit(task, Priority.NORMAL);
    }

    public Future<?> submit(Callable<?> task, Priority priority) {
        switch (priority) {
            case LOW:
                return lowPriorityPool.submit(task);
            case HIGH:
                return highPriorityPool.submit(task);
            default:
                return normalPriorityPool.submit(task);
        }
    }

    public Future<?> submit(Runnable task) {
        return submit(task, Priority.NORMAL);
    }

    public Future<?> submit(Runnable task, Priority priority) {
        switch (priority) {
            case LOW:
                return lowPriorityPool.submit(task);
            case HIGH:
                return highPriorityPool.submit(task);
            default:
                return normalPriorityPool.submit(task);
        }
    }

}
