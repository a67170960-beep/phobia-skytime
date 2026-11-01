package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.item.ItemStack;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class HotbarItemRenderEvent implements Event {
   private ItemStack stack;
   private final int hotbarIndex;

   public HotbarItemRenderEvent(ItemStack stack, int hotbarIndex) {
      this.stack = stack;
      this.hotbarIndex = hotbarIndex;
   }

   @Generated
   public ItemStack getStack() {
      return this.stack;
   }

   @Generated
   public int getHotbarIndex() {
      return this.hotbarIndex;
   }

   @Generated
   public void setStack(ItemStack stack) {
      this.stack = stack;
   }
}
