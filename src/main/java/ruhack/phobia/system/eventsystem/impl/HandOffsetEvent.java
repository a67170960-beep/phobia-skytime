package ruhack.phobia.system.eventsystem.impl;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import ruhack.phobia.system.eventsystem.api.events.callables.EventCancellable;

public class HandOffsetEvent extends EventCancellable {
   private MatrixStack matrices;
   private ItemStack stack;
   private Hand hand;
   private float scale;

   public HandOffsetEvent(MatrixStack matrices, ItemStack stack, Hand hand) {
      this.matrices = matrices;
      this.stack = stack;
      this.hand = hand;
      this.scale = 1.0F;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
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
   public float getScale() {
      return this.scale;
   }

   @Generated
   public void setMatrices(MatrixStack matrices) {
      this.matrices = matrices;
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
   public void setScale(float scale) {
      this.scale = scale;
   }
}
