package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class PushEvent extends EventCancellable {
   private PushEvent.Type type;

   @Generated
   public PushEvent.Type getType() {
      return this.type;
   }

   @Generated
   public PushEvent(PushEvent.Type type) {
      this.type = type;
   }

   public enum Type {
      COLLISION,
      BLOCK,
      WATER;
   }
}
