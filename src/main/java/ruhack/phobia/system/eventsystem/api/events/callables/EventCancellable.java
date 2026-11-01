package ruhack.phobia.system.eventsystem.api.events.callables;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.Cancellable;
import ruhack.phobia.system.eventsystem.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {
   private boolean cancelled;

   protected EventCancellable() {
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void cancel() {
      this.cancelled = true;
   }

   @Generated
   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
