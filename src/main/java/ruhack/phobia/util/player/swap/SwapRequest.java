package ruhack.phobia.util.player.swap;

public class SwapRequest implements Comparable<SwapRequest> {
   private final String id;
   private final int priority;
   private final Runnable swapAction;
   private final SwapSettings settings;
   private final Runnable onComplete;
   private final long queueTime;
   private final SwapExecutor executor;
   private boolean cancelled = false;
   private boolean started = false;

   public SwapRequest(String id, int priority, Runnable swapAction, SwapSettings settings, Runnable onComplete) {
      this.id = id;
      this.priority = priority;
      this.swapAction = swapAction;
      this.settings = settings != null ? settings : SwapSettings.instant();
      this.onComplete = onComplete;
      this.queueTime = System.currentTimeMillis();
      this.executor = new SwapExecutor();
   }

   public void start() {
      if (!this.cancelled) {
         if (InventoryUtils.hasCursorStack()) {
            InventoryUtils.dropCursorStack();
         }

         this.started = true;
         this.executor.execute(this.swapAction, this.settings, this::complete);
      }
   }

   public void tick() {
      if (!this.cancelled && this.started) {
         this.executor.tick();
      }
   }

   public void cancel() {
      this.cancelled = true;
      if (this.started) {
         this.executor.cancel();
      }

      if (this.onComplete != null) {
         try {
            this.onComplete.run();
         } catch (Exception var2) {
         }
      }
   }

   private void complete() {
      if (this.onComplete != null && !this.cancelled) {
         this.onComplete.run();
      }
   }

   public boolean isFinished() {
      return this.cancelled || this.started && !this.executor.isRunning();
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public boolean isStarted() {
      return this.started;
   }

   public String getId() {
      return this.id;
   }

   public int getPriority() {
      return this.priority;
   }

   public long getQueueTime() {
      return this.queueTime;
   }

   public long getAge() {
      return System.currentTimeMillis() - this.queueTime;
   }

   public int compareTo(SwapRequest other) {
      int priorityCompare = Integer.compare(this.priority, other.priority);
      return priorityCompare != 0 ? priorityCompare : Long.compare(this.queueTime, other.queueTime);
   }

   @Override
   public String toString() {
      return "SwapRequest{id='" + this.id + "', priority=" + this.priority + ", started=" + this.started + ", cancelled=" + this.cancelled + "}";
   }
}
