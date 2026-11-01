package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.events.Event;

public class ItemRendererEvent implements Event {
   private AbstractClientPlayerEntity player;
   private ItemStack stack;
   private Hand hand;

   @Generated
   public AbstractClientPlayerEntity getPlayer() {
      return this.player;
   }

   @Generated
   public ItemStack getStack() {
      return this.stack;
   }

   @Generated
   public Hand getHand() {
      return this.hand;
   }

   @Generated
   public void setPlayer(AbstractClientPlayerEntity player) {
      this.player = player;
   }

   @Generated
   public void setStack(ItemStack stack) {
      this.stack = stack;
   }

   @Generated
   public void setHand(Hand hand) {
      this.hand = hand;
   }

   @Generated
   public ItemRendererEvent(AbstractClientPlayerEntity player, ItemStack stack, Hand hand) {
      this.player = player;
      this.stack = stack;
      this.hand = hand;
   }
}
