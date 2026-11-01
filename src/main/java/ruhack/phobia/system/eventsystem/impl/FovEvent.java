package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class FovEvent extends EventCancellable {
   private int fov;

   @Generated
   public int getFov() {
      return this.fov;
   }

   @Generated
   public void setFov(int fov) {
      this.fov = fov;
   }
}
