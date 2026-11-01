package ruhack.phobia.system.modulesystem.impl.movement;

import com.adl.nativeprotect.Native;
import lombok.Generated;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.BlockPos;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.timer.StopWatch;

public class HighJump extends ModuleStructure {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Режим прыжка", "Boat", "Boat", "Shulker Screen", "Slime Boost", "FunTime Soul Sand");
   private boolean wasInShulkerScreen = false;
   private boolean wasOnSlimeBlock = false;
   private final StopWatch timer = new StopWatch();

   public HighJump() {
      super("HighJump", "Увеличивает высоту прыжка", ModuleCategory.MOVEMENT);
      this.settings(this.modeSetting);
   }

   @EventHandler
   @Native
   private void tickEvent(TickEvent event) {
      if (this.modeSetting.isSelected("Shulker Screen")) {
         this.handleShulkerScreen();
      }

      if (this.modeSetting.isSelected("FunTime Soul Sand") && mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
         mc.player.addVelocity(0.0, 0.56, 0.0);
      }

      if (this.modeSetting.isSelected("Boat")) {
         this.handleBoatMode();
      }

      if (this.modeSetting.isSelected("Slime Boost")) {
         this.handleSlimeBoost();
      }
   }

   @Native
   private void handleShulkerScreen() {
      if (mc.currentScreen instanceof ShulkerBoxScreen) {
         new StopWatch();
         float speed = 0.9F;
         mc.player.addVelocity(0.0, speed, 0.0);
      }
   }

   @Native
   private void handleBoatMode() {
      if (mc.currentScreen instanceof ShulkerBoxScreen) {
         float yaw = (float)Math.toRadians(mc.player.getYaw());
         double x = -Math.sin(yaw) * 1.0;
         double z = Math.cos(yaw) * 1.0;
         mc.player.addVelocity(0.0, 1.0, 0.0);
         mc.player.setPos(mc.player.getX(), mc.player.getY() + 0.24, mc.player.getZ());
      }

      if (mc.currentScreen instanceof ShulkerBoxScreen) {
         this.wasInShulkerScreen = true;
      } else if (this.wasInShulkerScreen && mc.currentScreen == null && this.isNearShulkerBox()) {
         this.wasInShulkerScreen = false;
      }
   }

   private void handleSlimeBoost() {
      if (mc.player.isOnGround() && this.isOnSlimeBlock()) {
         this.wasOnSlimeBlock = true;
      } else if (this.wasOnSlimeBlock && !mc.player.isOnGround() && mc.player.getVelocity().getY() > 0.0) {
         mc.player.addVelocity(0.0, 1.35, 0.0);
         this.wasOnSlimeBlock = false;
      } else if (!this.isOnSlimeBlock()) {
         this.wasOnSlimeBlock = false;
      }
   }

   @Native
   private boolean isNearShulkerBox() {
      BlockPos playerPos = mc.player.getBlockPos();

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isOnSlimeBlock() {
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos belowPos = playerPos.down();
      return mc.world.getBlockState(belowPos).getBlock() instanceof SlimeBlock;
   }

   @Generated
   public SelectSetting getModeSetting() {
      return this.modeSetting;
   }

   @Generated
   public boolean isWasInShulkerScreen() {
      return this.wasInShulkerScreen;
   }

   @Generated
   public boolean isWasOnSlimeBlock() {
      return this.wasOnSlimeBlock;
   }

   @Generated
   public StopWatch getTimer() {
      return this.timer;
   }
}
