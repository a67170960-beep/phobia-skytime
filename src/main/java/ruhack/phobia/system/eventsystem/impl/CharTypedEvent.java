package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class CharTypedEvent extends EventCancellable {
   private final char chr;

   public CharTypedEvent(char chr) {
      this.chr = chr;
   }

   @Generated
   public char getChr() {
      return this.chr;
   }
}
