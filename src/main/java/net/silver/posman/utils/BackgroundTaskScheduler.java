package net.silver.posman.utils;

import net.silver.log.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class BackgroundTaskScheduler {

  // Initialize a pool with a size equal to the available physical CPU cores.
  // This allows the scheduler to fully utilize the Ryzen 9 9950X's power.
  //  private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
  private static final int NUM_THREADS = 4;
  private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

  // Call this method to submit any background task
  public static void submitTask(Runnable task) {
    executor.submit(task);
  }

  // Call this method when the application shuts down
  public static void shutdown() {
    Log.info("Shutting down Application Task Scheduler...");
    executor.shutdown(); // Stop accepting new tasks
    try {
      // Wait for currently running tasks to finish (up to 30 seconds)
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // Forcefully shut down if tasks haven't finished
        Log.warn("Task Scheduler forcefully terminated due to timeout.");
      }
    } catch (InterruptedException e) {
      executor.shutdownNow(); // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}
