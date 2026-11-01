package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class HandledScreenEvent extends EventCancellable {
   private DrawContext drawContext;
   private Slot slotHover;
   private int mouseX;
   private int mouseY;
   private int backgroundWidth;
   private int backgroundHeight;

   @Generated
   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   @Generated
   public Slot getSlotHover() {
      return this.slotHover;
   }

   @Generated
   public int getMouseX() {
      return this.mouseX;
   }

   @Generated
   public int getMouseY() {
      return this.mouseY;
   }

   @Generated
   public int getBackgroundWidth() {
      return this.backgroundWidth;
   }

   @Generated
   public int getBackgroundHeight() {
      return this.backgroundHeight;
   }

   @Generated
   public HandledScreenEvent(DrawContext drawContext, Slot slotHover, int mouseX, int mouseY, int backgroundWidth, int backgroundHeight) {
      this.drawContext = drawContext;
      this.slotHover = slotHover;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.backgroundWidth = backgroundWidth;
      this.backgroundHeight = backgroundHeight;
   }
}
