package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.item.ItemStack;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class HeldItemUpdateEvent implements Event {
   private ItemStack mainHand;
   private ItemStack offHand;

   @Generated
   public ItemStack getMainHand() {
      return this.mainHand;
   }

   @Generated
   public ItemStack getOffHand() {
      return this.offHand;
   }

   @Generated
   public void setMainHand(ItemStack mainHand) {
      this.mainHand = mainHand;
   }

   @Generated
   public void setOffHand(ItemStack offHand) {
      this.offHand = offHand;
   }

   @Generated
   public HeldItemUpdateEvent(ItemStack mainHand, ItemStack offHand) {
      this.mainHand = mainHand;
      this.offHand = offHand;
   }
}
