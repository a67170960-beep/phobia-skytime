package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class DeathScreenEvent implements Event {
   private int ticksSinceDeath;

   @Generated
   public int getTicksSinceDeath() {
      return this.ticksSinceDeath;
   }

   @Generated
   public DeathScreenEvent(int ticksSinceDeath) {
      this.ticksSinceDeath = ticksSinceDeath;
   }
}
