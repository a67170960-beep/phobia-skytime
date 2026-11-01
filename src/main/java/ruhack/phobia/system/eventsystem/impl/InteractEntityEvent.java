package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.entity.Entity;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class InteractEntityEvent extends EventCancellable {
   private Entity entity;

   @Generated
   public InteractEntityEvent(Entity entity) {
      this.entity = entity;
   }

   @Generated
   public Entity getEntity() {
      return this.entity;
   }

   @Generated
   public void setEntity(Entity entity) {
      this.entity = entity;
   }
}
