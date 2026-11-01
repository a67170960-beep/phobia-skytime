package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class UsingItemEvent extends EventCancellable {
   byte type;

   @Generated
   public byte getType() {
      return this.type;
   }

   @Generated
   public void setType(byte type) {
      this.type = type;
   }

   @Generated
   public UsingItemEvent(byte type) {
      this.type = type;
   }
}
