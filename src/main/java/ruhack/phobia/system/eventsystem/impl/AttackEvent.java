package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.entity.Entity;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class AttackEvent implements Event {
   private final Entity target;

   @Generated
   public Entity getTarget() {
      return this.target;
   }

   @Generated
   public AttackEvent(Entity target) {
      this.target = target;
   }
}
