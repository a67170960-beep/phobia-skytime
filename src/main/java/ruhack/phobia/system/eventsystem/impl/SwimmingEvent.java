package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class SwimmingEvent extends EventCancellable {
   Vec3d vector;

   @Generated
   public void setVector(Vec3d vector) {
      this.vector = vector;
   }

   @Generated
   public Vec3d getVector() {
      return this.vector;
   }

   @Generated
   public SwimmingEvent(Vec3d vector) {
      this.vector = vector;
   }
}
