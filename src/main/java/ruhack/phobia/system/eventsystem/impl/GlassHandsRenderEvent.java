package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class GlassHandsRenderEvent extends EventCancellable {
   private GlassHandsRenderEvent.Phase phase;
   private MatrixStack matrices;
   private float tickDelta;

   @Generated
   public GlassHandsRenderEvent(GlassHandsRenderEvent.Phase phase, MatrixStack matrices, float tickDelta) {
      this.phase = phase;
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   @Generated
   public GlassHandsRenderEvent.Phase getPhase() {
      return this.phase;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }

   @Generated
   public void setPhase(GlassHandsRenderEvent.Phase phase) {
      this.phase = phase;
   }

   @Generated
   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
   }

   @Generated
   public void setTickDelta(float tickDelta) {
      this.tickDelta = tickDelta;
   }

   public enum Phase {
      PRE,
      POST;
   }
}
