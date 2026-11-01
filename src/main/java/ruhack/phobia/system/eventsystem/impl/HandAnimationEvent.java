package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class HandAnimationEvent extends EventCancellable {
   private MatrixStack matrices;
   private Hand hand;
   private float swingProgress;

   @Generated
   public HandAnimationEvent(MatrixStack matrices, Hand hand, float swingProgress) {
      this.matrices = matrices;
      this.hand = hand;
      this.swingProgress = swingProgress;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public Hand getHand() {
      return this.hand;
   }

   @Generated
   public float getSwingProgress() {
      return this.swingProgress;
   }

   @Generated
   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   @Generated
   public void setHand(Hand hand) {
      this.hand = hand;
   }

   @Generated
   public void setSwingProgress(float swingProgress) {
      this.swingProgress = swingProgress;
   }
}
