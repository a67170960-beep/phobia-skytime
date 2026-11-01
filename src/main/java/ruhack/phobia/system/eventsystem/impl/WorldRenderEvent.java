package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class WorldRenderEvent implements Event {
   private MatrixStack stack;
   private float partialTicks;

   @Generated
   public WorldRenderEvent(MatrixStack stack, float partialTicks) {
      this.stack = stack;
      this.partialTicks = partialTicks;
   }

   @Generated
   public MatrixStack getStack() {
      return this.stack;
   }

   @Generated
   public float getPartialTicks() {
      return this.partialTicks;
   }
}
