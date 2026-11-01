package ruhack.phobia.system.modulesystem.impl.movement;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.SwimmingEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.rotation.RotationController;

public class WaterSpeed extends ModuleStructure {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим обхода", "FunTime", "FunTime");
   private final BooleanSetting iceBoost = new BooleanSetting("Ускорение под льдом", "Ускоряет когда упираешься головой в лёд").setValue(true);
   private final SliderSettings iceBoostSpeed = new SliderSettings("Скорость под льдом", "Множитель скорости под льдом", 1.5F)
      .range(1.0F, 3.0F)
      .visible(() -> this.iceBoost.isValue());

   public WaterSpeed() {
      super("WaterSpeed", "Ускоряет игрока в воде", ModuleCategory.MOVEMENT);
      this.settings(this.modeSetting, this.iceBoost, this.iceBoostSpeed);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.modeSetting.isSelected("FunTime") && mc.player.isSwimming() && mc.player.isOnGround()) {
            mc.player.jump();
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
         }

         if (this.iceBoost.isValue() && mc.player.isSwimming() && this.isHeadUnderIce()) {
            this.applyIceBoost();
         }
      }
   }

   private void applyIceBoost() {
      float speedMultiplier = this.iceBoostSpeed.getValue();
      double yaw = Math.toRadians(mc.player.getYaw());
      double pitch = Math.toRadians(mc.player.getPitch());
      double baseSpeed = 0.04 * speedMultiplier;
      double horizontalSpeed = Math.cos(pitch) * baseSpeed;
      double motionX = -Math.sin(yaw) * horizontalSpeed;
      double motionZ = Math.cos(yaw) * horizontalSpeed;
      mc.player.setVelocity(mc.player.getVelocity().x + motionX, mc.player.getVelocity().y, mc.player.getVelocity().z + motionZ);
   }

   @EventHandler
   public void onSwimming(SwimmingEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.modeSetting.isSelected("FunTime")) {
            this.processSwimmingBoost(e);
         }
      }
   }

   private void processSwimmingBoost(SwimmingEvent e) {
      if (mc.options.jumpKey.isPressed()) {
         float pitch = RotationController.INSTANCE.getRotation().getPitch();
         float boost = pitch >= 0.0F ? MathHelper.clamp(pitch / 45.0F, 1.0F, 2.0F) : 0.5F;
         e.getVector().y = 0.1 * boost;
      }

      if (this.iceBoost.isValue() && this.isHeadUnderIce()) {
         float speedMultiplier = this.iceBoostSpeed.getValue();
         e.getVector().x *= speedMultiplier;
         e.getVector().z *= speedMultiplier;
      }
   }

   private boolean isHeadUnderIce() {
      if (mc.player != null && mc.world != null) {
         BlockPos headPos = mc.player.getBlockPos().up(1);
         BlockPos aboveHeadPos = mc.player.getBlockPos().up(2);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               BlockPos checkPos = headPos.add(dx, 0, dz);
               BlockPos checkPosAbove = aboveHeadPos.add(dx, 0, dz);
               if (this.isIceBlock(checkPos) || this.isIceBlock(checkPosAbove)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isIceBlock(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE || block == Blocks.FROSTED_ICE;
   }
}
