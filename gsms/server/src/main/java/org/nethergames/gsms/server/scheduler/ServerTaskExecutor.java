package org.nethergames.gsms.server.scheduler;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for scheduling tasks
 */
@Log4j2(topic = "Task Scheduler")
public class ServerTaskExecutor {
    private static int taskId = 0;
    private static int currentTick = 0;
    private static final Set<TaskMetadata> tasks = new LinkedHashSet<>();

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new ServerThreadExecutor());

    public void tick() {
        currentTick++;

        tasks.stream().filter(i -> (currentTick - i.getLastTick() - i.getDelay()) >= 0).forEachOrdered(task -> {
            boolean remove = false;
            try {
                if (task.isAsync()) {
                    executorService.execute(task.getRunnable());
                } else {
                    task.getRunnable().run();
                }
            } catch (Throwable error) {
                if (!(error instanceof TaskCancelException)) {
                    log.error("Error thrown while executing tasks", error);
                }

                remove = true;
            }

            if (!task.isRepeating || remove) {
                tasks.remove(task);
            } else {
                task.delay = task.getPeriod();
                task.lastTick = currentTick;
            }
        });
    }

    @SneakyThrows
    public void shutdown() {
        executorService.shutdown();
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
            log.warn("Unable to shutdown executor service gracefully.");
        }
    }

    @NotNull
    public static TaskMetadata schedule(Runnable task, long delay, TimeUnit unit) {
        TaskMetadata metadata = new TaskMetadata();
        metadata.runnable = task;
        metadata.delay = unit.toMillis(delay) / 50;
        metadata.lastTick = currentTick;

        tasks.add(metadata);

        return metadata;
    }

    @NotNull
    public static TaskMetadata scheduleRepeating(Runnable task, long delay, long period, TimeUnit unit) {
        return scheduleRepeating(task, delay, period, unit, false);
    }

    @NotNull
    public static TaskMetadata scheduleRepeating(Runnable task, long delay, long period, TimeUnit unit, boolean async) {
        TaskMetadata metadata = new TaskMetadata();
        metadata.runnable = task;
        metadata.delay = unit.toMillis(delay) / 50;
        metadata.period = unit.toMillis(period) / 50;
        metadata.lastTick = currentTick;
        metadata.isRepeating = true;
        metadata.async = async;

        tasks.add(metadata);

        return metadata;
    }

    public static class TaskCancelException extends RuntimeException {

    }

    @Getter
    public static class TaskMetadata {
        private final int taskId = ServerTaskExecutor.taskId++;

        private Runnable runnable;
        private long lastTick;
        private long delay = 0;
        private long period = 0;
        private boolean isRepeating = false;
        private boolean async = false;
    }

    private static class ServerThreadExecutor implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, "Asynchronous Thread Executor #" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
