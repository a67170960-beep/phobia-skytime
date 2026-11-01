package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class RotationUpdateEvent implements Event {
   byte type;

   @Generated
   public byte getType() {
      return this.type;
   }

   @Generated
   public RotationUpdateEvent(byte type) {
      this.type = type;
   }
}
