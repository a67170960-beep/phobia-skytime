package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class CameraEvent extends EventCancellable {
   private boolean cameraClip;
   private float distance;

   @Generated
   public boolean isCameraClip() {
      return this.cameraClip;
   }

   @Generated
   public float getDistance() {
      return this.distance;
   }

   @Generated
   public void setCameraClip(boolean cameraClip) {
      this.cameraClip = cameraClip;
   }

   @Generated
   public void setDistance(float distance) {
      this.distance = distance;
   }

   @Generated
   public CameraEvent(boolean cameraClip, float distance) {
      this.cameraClip = cameraClip;
      this.distance = distance;
   }
}
