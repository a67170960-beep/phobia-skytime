package ruhack.phobia.system.eventsystem.api.events.callables;

import ruhack.phobia.system.eventsystem.api.events.Event;
import ruhack.phobia.system.eventsystem.api.events.Typed;

public abstract class EventTyped implements Event, Typed {
   private final byte type;

   protected EventTyped(byte eventType) {
      this.type = eventType;
   }

   @Override
   public byte getType() {
      return this.type;
   }
}
