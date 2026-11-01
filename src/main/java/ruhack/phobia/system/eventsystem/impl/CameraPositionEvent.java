package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class CameraPositionEvent implements Event {
   private Vec3d pos;

   @Generated
   public Vec3d getPos() {
      return this.pos;
   }

   @Generated
   public void setPos(Vec3d pos) {
      this.pos = pos;
   }

   @Generated
   public CameraPositionEvent(Vec3d pos) {
      this.pos = pos;
   }
}
