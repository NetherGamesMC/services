package org.nethergames.gsms.server;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.server.impl.GameService;
import org.nethergames.gsms.server.manager.ProxyManager;
import org.nethergames.gsms.server.scheduler.ServerTaskExecutor;
import org.nethergames.gsms.server.server.ServerRegistry;
import org.nethergames.gsms.server.web.WebServer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
@Log4j2(topic = "GSMS")
public class GSMS {
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private final ProxyManager proxyManager;
    private final ServerRegistry serverRegistry;
    private final WebServer webServer;
    private final ServerTaskExecutor taskManager;
    private final GameService grpcServer;

    private final Queue<Runnable> notifications = new ConcurrentLinkedQueue<>();

    public GSMS() {
        log.info("Starting GSMS (GameServer Management System)");

        var mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning.compareAndSet(true, false);

            try {
                mainThread.join();
            } catch (Throwable e) {
                log.error("An exception was thrown while handling shutdown signal.", e);
            }
        }));

        taskManager = new ServerTaskExecutor();
        proxyManager = new ProxyManager(this);
        serverRegistry = new ServerRegistry(6, 2, this);
        grpcServer = new GameService(this, 5001);
        serverRegistry.initRegistry();
        webServer = new WebServer(serverRegistry, proxyManager);
    }

    public void start(Consumer<GSMS> startHook) {
        startHook.accept(this);

        while (isRunning.get()) {
            var lastTick = System.currentTimeMillis();

            taskManager.tick();

            var diffInTicks = System.currentTimeMillis() - lastTick;

            if (diffInTicks < 50) {
                sleepThread(lastTick, 50 - diffInTicks);
            }
        }

        shutdown();
    }

    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            return;
        }

        if (!isShutdown.compareAndSet(false, true)) {
            throw new IllegalCallerException("Trying to call shutdown twice.");
        }

        try {
            log.warn("Stopping GSMS gracefully.");

            webServer.shutdown();
            taskManager.shutdown();
            serverRegistry.shutdown();
            grpcServer.shutdown();

            log.warn("GSMS service has been gracefully stopped, program exited.");
        } catch (Throwable error) {
            log.error("Unable to shutdown GSMS service gracefully", error);
        }
    }

    /**
     * Multithreading functionality, wait for notifications from another thread. This will help
     * to reduce the time needed for a task to execute in the main thread.
     *
     * @param startSleeper The time of the sleep being executed.
     * @param millis       The time required for the thread to sleep.
     */
    private synchronized void sleepThread(long startSleeper, long millis) {
        try {
            long sleepTime;
            while ((sleepTime = (System.currentTimeMillis() - startSleeper)) < millis) {
                wait(millis - sleepTime);

                Runnable runnable;
                while ((runnable = notifications.poll()) != null) {
                    runnable.run();
                }
            }
        } catch (InterruptedException e) {
            log.error("Error occurred while trying to sleep", e);
        }
    }

    /**
     * Perform a callback action to the main thread, everything in this closure will
     * be executed within the main thread.
     */
    public synchronized void wakeupThread(Runnable executor) {
        notifications.add(executor);

        notify();
    }
}
