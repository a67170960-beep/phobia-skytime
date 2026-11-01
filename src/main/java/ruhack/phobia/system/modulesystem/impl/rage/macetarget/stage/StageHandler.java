package ruhack.phobia.system.modulesystem.impl.rage.macetarget.stage;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.armor.ArmorSwapHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.armor.FireworkHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.attack.AttackHandler;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.state.MaceState;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.timer.StopWatch;

public class StageHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final ArmorSwapHandler armorSwapHandler;
   private final FireworkHandler fireworkHandler;
   private final AttackHandler attackHandler;
   private final StopWatch fireworkTimer;
   private MaceState.Stage stage = MaceState.Stage.PREPARE;
   private boolean silentMode = true;
   private boolean reallyWorldMode = false;
   private float height = 30.0F;

   public StageHandler(ArmorSwapHandler armorSwapHandler, FireworkHandler fireworkHandler, AttackHandler attackHandler, StopWatch fireworkTimer) {
      this.armorSwapHandler = armorSwapHandler;
      this.fireworkHandler = fireworkHandler;
      this.attackHandler = attackHandler;
      this.fireworkTimer = fireworkTimer;
   }

   public void handlePrepare(boolean hasElytra) {
      if (!hasElytra) {
         int slot = InventoryUtils.findElytraSlot();
         if (slot != -1) {
            this.armorSwapHandler.startSwap(slot, this.silentMode);
         }
      } else {
         this.stage = MaceState.Stage.FLYING_UP;
         this.fireworkTimer.reset();
      }
   }

   public void handleFlyingUp(LivingEntity target, boolean hasElytra) {
      if (!hasElytra) {
         this.stage = MaceState.Stage.PREPARE;
      } else {
         if (mc.player.isGliding() && this.fireworkTimer.finished(300.0)) {
            this.fireworkHandler.useFirework(this.silentMode);
            this.fireworkTimer.reset();
         }

         if (mc.player.getY() - target.getY() >= this.height) {
            this.stage = MaceState.Stage.TARGETTING;
         }
      }
   }

   public void handleTargetting(LivingEntity target) {
      float swapDistance = 12.0F;
      if (InventoryUtils.hasElytra() && mc.player.distanceTo(target) < swapDistance && !this.armorSwapHandler.isActive()) {
         int slot = InventoryUtils.findChestArmorSlot();
         if (slot != -1) {
            this.armorSwapHandler.startSwap(slot, this.silentMode);
         }
      }

      if (mc.player.distanceTo(target) < 16.0F) {
         this.stage = MaceState.Stage.ATTACKING;
      }
   }

   public void handleAttacking(LivingEntity target, boolean hasElytra) {
      if (hasElytra && !this.armorSwapHandler.isActive()) {
         int slot = InventoryUtils.findChestArmorSlot();
         if (slot != -1) {
            this.armorSwapHandler.startSwap(slot, this.silentMode);
            return;
         }
      }

      if (!this.armorSwapHandler.isActive()) {
         this.attackHandler.holdMace();
         if (mc.player.isOnGround() && mc.player.distanceTo(target) > 6.0F) {
            this.stage = MaceState.Stage.FLYING_UP;
            this.fireworkTimer.reset();
         }
      }
   }

   public void reset() {
      this.stage = MaceState.Stage.PREPARE;
   }

   @Generated
   public ArmorSwapHandler getArmorSwapHandler() {
      return this.armorSwapHandler;
   }

   @Generated
   public FireworkHandler getFireworkHandler() {
      return this.fireworkHandler;
   }

   @Generated
   public AttackHandler getAttackHandler() {
      return this.attackHandler;
   }

   @Generated
   public StopWatch getFireworkTimer() {
      return this.fireworkTimer;
   }

   @Generated
   public MaceState.Stage getStage() {
      return this.stage;
   }

   @Generated
   public boolean isSilentMode() {
      return this.silentMode;
   }

   @Generated
   public boolean isReallyWorldMode() {
      return this.reallyWorldMode;
   }

   @Generated
   public float getHeight() {
      return this.height;
   }

   @Generated
   public void setStage(MaceState.Stage stage) {
      this.stage = stage;
   }

   @Generated
   public void setSilentMode(boolean silentMode) {
      this.silentMode = silentMode;
   }

   @Generated
   public void setReallyWorldMode(boolean reallyWorldMode) {
      this.reallyWorldMode = reallyWorldMode;
   }

   @Generated
   public void setHeight(float height) {
      this.height = height;
   }
}
