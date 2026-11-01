package ruhack.phobia.system.modulesystem.impl.rage.macetarget.attack;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.util.player.swap.InventoryUtils;

public class AttackHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final double ATTACK_RANGE = 4.5;
   private static final double MAX_ATTACK_RANGE = 6.0;
   private int savedSlot = -1;

   public void holdMace() {
      if (mc.player != null) {
         if (mc.player.getMainHandStack().getItem() != Items.MACE) {
            int hotbarSlot = InventoryUtils.findItemInHotbar(Items.MACE);
            if (hotbarSlot != -1) {
               if (this.savedSlot == -1) {
                  this.savedSlot = mc.player.getInventory().getSelectedSlot();
               }

               InventoryUtils.selectSlot(hotbarSlot);
            } else {
               int invSlot = InventoryUtils.findItemInInventory(Items.MACE);
               if (invSlot != -1) {
                  InventoryUtils.click(InventoryUtils.wrapSlot(invSlot), mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP);
                  InventoryUtils.closeScreen();
               }
            }
         }
      }
   }

   public void releaseMace() {
      if (this.savedSlot != -1 && mc.player != null) {
         InventoryUtils.selectSlot(this.savedSlot);
      }

      this.savedSlot = -1;
   }

   public boolean shouldAttack(LivingEntity target) {
      if (mc.player != null && target != null) {
         double dist = mc.player.distanceTo(target);
         if (dist < 4.5) {
            return true;
         } else if (dist < 6.0) {
            Vec3d selfNext = mc.player.getEntityPos().add(mc.player.getVelocity());
            Vec3d targetNext = target.getEntityPos().add(target.getVelocity());
            return selfNext.distanceTo(targetNext) > dist;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void performAttack(LivingEntity target) {
      if (mc.player != null && target != null && mc.interactionManager != null && mc.getNetworkHandler() != null) {
         if (mc.player.getMainHandStack().getItem() == Items.MACE) {
            this.attack(target);
         } else {
            int hotbarSlot = InventoryUtils.findItemInHotbar(Items.MACE);
            if (hotbarSlot != -1) {
               this.attackWithHotbarMace(hotbarSlot, target);
            } else {
               int invSlot = InventoryUtils.findItemInInventory(Items.MACE);
               if (invSlot != -1) {
                  this.attackWithInventoryMace(invSlot, target);
               } else if (mc.player.getOffHandStack().getItem() == Items.MACE) {
                  this.attackWithOffhandMace(target);
               } else {
                  this.attack(target);
               }
            }
         }
      }
   }

   private void attackWithHotbarMace(int maceSlot, LivingEntity target) {
      int prevSlot = mc.player.getInventory().getSelectedSlot();
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
      this.attack(target);
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
   }

   private void attackWithInventoryMace(int invSlot, LivingEntity target) {
      int selected = mc.player.getInventory().getSelectedSlot();
      int screenSlot = InventoryUtils.wrapSlot(invSlot);
      InventoryUtils.click(screenSlot, selected, SlotActionType.SWAP);
      this.attack(target);
      InventoryUtils.click(screenSlot, selected, SlotActionType.SWAP);
      InventoryUtils.closeScreen();
   }

   private void attackWithOffhandMace(LivingEntity target) {
      int selectedScreenSlot = InventoryUtils.wrapSlot(mc.player.getInventory().getSelectedSlot());
      InventoryUtils.click(selectedScreenSlot, 40, SlotActionType.SWAP);
      this.attack(target);
      InventoryUtils.click(selectedScreenSlot, 40, SlotActionType.SWAP);
      InventoryUtils.closeScreen();
   }

   private void attack(LivingEntity target) {
      mc.interactionManager.attackEntity(mc.player, target);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public void reset() {
      this.savedSlot = -1;
   }

   @Generated
   public int getSavedSlot() {
      return this.savedSlot;
   }

   @Generated
   public void setSavedSlot(int savedSlot) {
      this.savedSlot = savedSlot;
   }
}
