package ruhack.phobia.system.modulesystem.impl.movement;

import com.adl.nativeprotect.Native;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.FireworkEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.rotation.RotationController;

public class FireworkBooster extends ModuleStructure {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима", "BravoHvH", "BravoHvH", "ReallyWorld", "PulseHVH", "Custom");
   private final SliderSettings customSpeedSetting = new SliderSettings("Скорость", "Скорость для Custom режима", 1.963F)
      .range(1.5F, 3.0F)
      .visible(() -> this.modeSetting.isSelected("Custom"));
   private final BooleanSetting nearBoostSetting = new BooleanSetting("", "");

   public FireworkBooster() {
      super("FireworkBooster", "Добавляет силу к тяге феерверка", ModuleCategory.MOVEMENT);
      this.settings(this.modeSetting, this.customSpeedSetting);
   }

   @EventHandler
   @Native
   public void onFirework(FireworkEvent e) {
      if (mc.player != null && mc.player.isGliding()) {
         float yaw = RotationController.INSTANCE.getRotation().getYaw() % 360.0F;
         if (yaw < 0.0F) {
            yaw += 360.0F;
         }

         if (this.modeSetting.isSelected("ReallyWorld")) {
            this.handleReallyWorldMode(e, yaw);
         } else if (this.modeSetting.isSelected("BravoHvH")) {
            this.handleBravoHvHMode(e, yaw);
         } else if (this.modeSetting.isSelected("PulseHVH")) {
            this.handlePulseHVHMode(e, yaw);
         } else if (this.modeSetting.isSelected("Custom")) {
            this.handleCustomMode(e, yaw);
         }
      }
   }

   @Native
   private void handleReallyWorldMode(FireworkEvent e, float yaw) {
      float[] diagonals = new float[]{45.0F, 135.0F, 225.0F, 315.0F};
      float closestDiff = 180.0F;

      for (float d : diagonals) {
         float diff = Math.abs(yaw - d);
         diff = Math.min(diff, 360.0F - diff);
         if (diff < closestDiff) {
            closestDiff = diff;
         }
      }

      double speedXZ = 1.5;
      double speedY = 1.5;
      if (closestDiff <= 4.0F) {
         speedXZ = 2.2;
      } else if (closestDiff <= 8.0F) {
         speedXZ = 2.06;
      } else if (closestDiff <= 12.0F) {
         speedXZ = 1.98;
      } else if (closestDiff <= 16.0F) {
         speedXZ = 1.87;
      } else if (closestDiff <= 20.0F) {
         speedXZ = 1.8;
      } else if (closestDiff <= 24.0F) {
         speedXZ = 1.74;
      } else if (closestDiff <= 28.0F) {
         speedXZ = 1.7;
      } else if (closestDiff <= 32.0F) {
         speedXZ = 1.65;
      } else if (closestDiff <= 36.0F) {
         speedXZ = 1.63;
      } else {
         speedXZ = 1.61;
         speedY = 1.61;
      }

      this.applyFireworkVelocity(e, speedXZ, speedY);
   }

   @Native
   private void handleBravoHvHMode(FireworkEvent e, float yaw) {
      boolean isDiagonal = this.checkDiagonal(yaw, 16.0F);
      boolean nearPlayer = this.checkNearPlayer(4.0F);
      double speedY = 1.66;
      double speedXZ;
      if (isDiagonal) {
         speedXZ = 1.963;
      } else if (this.nearBoostSetting.isValue() && nearPlayer) {
         speedXZ = 1.82;
         speedY = 1.67;
      } else {
         speedXZ = 1.675;
      }

      this.applyFireworkVelocity(e, speedXZ, speedY);
   }

   private void handlePulseHVHMode(FireworkEvent e, float yaw) {
      boolean isDiagonal = this.checkDiagonal(yaw, 16.0F);
      boolean nearPlayer = this.checkNearPlayer(5.0F);
      double speedY = 1.66;
      double speedXZ;
      if (isDiagonal) {
         speedXZ = 1.963;
      } else if (this.nearBoostSetting.isValue() && nearPlayer) {
         speedXZ = 1.82;
         speedY = 1.67;
      } else {
         speedXZ = 1.675;
      }

      this.applyFireworkVelocity(e, speedXZ, speedY);
   }

   private void handleCustomMode(FireworkEvent e, float yaw) {
      boolean isDiagonal = this.checkDiagonal(yaw, 16.0F);
      boolean nearPlayer = this.checkNearPlayer(5.0F);
      double speedY = 1.66;
      double speedXZ;
      if (isDiagonal) {
         speedXZ = this.customSpeedSetting.getValue();
      } else if (this.nearBoostSetting.isValue() && nearPlayer) {
         speedXZ = this.customSpeedSetting.getValue() - 0.1F;
         speedY = 1.67;
      } else {
         speedXZ = 1.675;
      }

      this.applyFireworkVelocity(e, speedXZ, speedY);
   }

   private boolean checkDiagonal(float yaw, float threshold) {
      for (float d : new float[]{45.0F, 135.0F, 225.0F, 315.0F}) {
         float diff = Math.abs(yaw - d);
         diff = Math.min(diff, 360.0F - diff);
         if (diff <= threshold) {
            return true;
         }
      }

      return false;
   }

   private boolean checkNearPlayer(float distance) {
      if (this.nearBoostSetting.isValue() && mc.world != null) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.distanceTo(mc.player) <= distance) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Native
   private void applyFireworkVelocity(FireworkEvent e, double speedXZ, double speedY) {
      Vec3d rotationVector = RotationController.INSTANCE.getMoveRotation().toVector();
      Vec3d currentVelocity = e.getVector();
      e.setVector(
         currentVelocity.add(
            rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
            rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
            rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
         )
      );
   }
}
