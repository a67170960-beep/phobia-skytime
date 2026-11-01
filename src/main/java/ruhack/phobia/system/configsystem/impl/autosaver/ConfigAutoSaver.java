package ruhack.phobia.system.configsystem.impl.autosaver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import ruhack.phobia.system.configsystem.impl.consolelogger.Logger;

public class ConfigAutoSaver {
   private static final long SAVE_INTERVAL_MS = 90000L;
   private static final long INITIAL_DELAY_MS = 90000L;
   private final ScheduledExecutorService executor;
   private final Runnable saveTask;
   private final AtomicBoolean running;
   private final AtomicLong lastSaveTime;
   private ScheduledFuture<?> scheduledTask;

   public ConfigAutoSaver(Runnable saveTask) {
      this.saveTask = saveTask;
      this.running = new AtomicBoolean(false);
      this.lastSaveTime = new AtomicLong(0L);
      this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread thread = new Thread(r, "Phobia-ConfigAutoSaver");
         thread.setDaemon(true);
         thread.setPriority(1);
         return thread;
      });
   }

   public void start() {
      if (this.running.compareAndSet(false, true)) {
         this.scheduledTask = this.executor.scheduleAtFixedRate(this::executeSave, 90000L, 90000L, TimeUnit.MILLISECONDS);
         Logger.info("AutoConfiguration: AutoSaver started (interval: 90s)");
      }
   }

   private void executeSave() {
      if (this.running.get()) {
         try {
            this.saveTask.run();
            this.lastSaveTime.set(System.currentTimeMillis());
         } catch (Exception e) {
            Logger.error("AutoConfiguration: AutoSave failed! " + e.getMessage());
         }
      }
   }

   public void stop() {
      this.running.set(false);
      if (this.scheduledTask != null) {
         this.scheduledTask.cancel(false);
      }
   }

   public void shutdown() {
      this.stop();
      this.executor.shutdown();

      try {
         if (!this.executor.awaitTermination(3L, TimeUnit.SECONDS)) {
            this.executor.shutdownNow();
         }
      } catch (InterruptedException e) {
         this.executor.shutdownNow();
         Thread.currentThread().interrupt();
      }
   }

   public long getLastSaveTime() {
      return this.lastSaveTime.get();
   }

   public boolean isRunning() {
      return this.running.get();
   }
}
