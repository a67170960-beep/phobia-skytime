package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class BoundingBoxControlEvent extends EventCancellable {
   public Box box;
   public Entity entity;

   @Generated
   public Box getBox() {
      return this.box;
   }

   @Generated
   public Entity getEntity() {
      return this.entity;
   }

   @Generated
   public void setBox(Box box) {
      this.box = box;
   }

   @Generated
   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   @Generated
   public BoundingBoxControlEvent(Box box, Entity entity) {
      this.box = box;
      this.entity = entity;
   }
}
