package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class DrawEvent implements Event {
   private DrawContext drawContext;
   private float partialTicks;

   @Generated
   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   @Generated
   public float getPartialTicks() {
      return this.partialTicks;
   }

   @Generated
   public DrawEvent(DrawContext drawContext, float partialTicks) {
      this.drawContext = drawContext;
      this.partialTicks = partialTicks;
   }
}
