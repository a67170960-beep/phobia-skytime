package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class HotBarScrollEvent extends EventCancellable {
   private double horizontal;
   private double vertical;

   @Generated
   public double getHorizontal() {
      return this.horizontal;
   }

   @Generated
   public double getVertical() {
      return this.vertical;
   }

   @Generated
   public void setHorizontal(double horizontal) {
      this.horizontal = horizontal;
   }

   @Generated
   public void setVertical(double vertical) {
      this.vertical = vertical;
   }

   @Generated
   public HotBarScrollEvent(double horizontal, double vertical) {
      this.horizontal = horizontal;
      this.vertical = vertical;
   }
}
