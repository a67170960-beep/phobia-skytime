package ruhack.phobia.util.math;

import java.util.PriorityQueue;
import lombok.Generated;
import ruhack.phobia.system.modulesystem.ModuleStructure;

public class TaskProcessor<T> {
   public int tickCounter = 0;
   public PriorityQueue<TaskProcessor.Task<T>> activeTasks = new PriorityQueue<>((r1, r2) -> Integer.compare(r2.priority, r1.priority));

   public void tick(int deltaTime) {
      this.tickCounter += deltaTime;
   }

   public void addTask(TaskProcessor.Task<T> task) {
      this.activeTasks.removeIf(r -> r.provider.equals(task.provider));
      task.expiresIn = task.expiresIn + this.tickCounter;
      this.activeTasks.add(task);
   }

   public T fetchActiveTaskValue() {
      while (
         !this.activeTasks.isEmpty()
            && this.activeTasks.peek() != null
            && (this.activeTasks.peek().expiresIn <= this.tickCounter || !this.activeTasks.peek().provider.isState())
      ) {
         this.activeTasks.poll();
      }

      if (this.activeTasks.isEmpty()) {
         return null;
      } else {
         return this.activeTasks.peek() != null ? this.activeTasks.peek().value : null;
      }
   }

   public static class Task<T> {
      private int expiresIn;
      private final int priority;
      private final ModuleStructure provider;
      private final T value;

      @Generated
      @Override
      public String toString() {
         return "TaskProcessor.Task(expiresIn="
            + this.expiresIn
            + ", priority="
            + this.priority
            + ", provider="
            + this.provider
            + ", value="
            + this.value
            + ")";
      }

      @Generated
      public Task(int expiresIn, int priority, ModuleStructure provider, T value) {
         this.expiresIn = expiresIn;
         this.priority = priority;
         this.provider = provider;
         this.value = value;
      }
   }
}
