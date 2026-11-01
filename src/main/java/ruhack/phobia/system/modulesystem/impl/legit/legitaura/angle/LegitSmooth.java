package ruhack.phobia.system.modulesystem.impl.legit.legitaura.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.modulesystem.impl.legit.LegitAura;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.utils.FromVector;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class LegitSmooth extends AngleSmoothMode {
   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;

   public LegitSmooth() {
      super("Legit");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (entity != null && LegitAura.getInstance().getPitchCorrecting().isValue()) {
         targetAngle = AngleUtil.calculateAngle(FromVector.brain(entity, 0.0F, 2.0F));
      }

      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      float straightLineYaw;
      float straightLinePitch;
      if (entity != null) {
         double angleDiff = Math.abs(yawDelta) + Math.abs(pitchDelta);
         float minYawSpeed = LegitAura.getInstance().getSpeed().getValue() / 4.0F;
         float maxYawSpeed = LegitAura.getInstance().getSpeed().getValue() / 2.0F;
         float minPitchSpeed = LegitAura.getInstance().getSpeed().getValue() / 24.0F;
         float maxPitchSpeed = LegitAura.getInstance().getSpeed().getValue() / 12.0F;
         if (entity == null) {
            minYawSpeed = 360.0F;
            maxYawSpeed = 360.0F;
            minPitchSpeed = 360.0F;
            maxPitchSpeed = 360.0F;
         }

         float factor = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)(angleDiff / rotationDifference) + 5.0F));
         straightLineYaw = minYawSpeed + (maxYawSpeed - minYawSpeed) * factor;
         straightLinePitch = minPitchSpeed + (maxPitchSpeed - minPitchSpeed) * factor;
         float straightLineReturnPitch = minPitchSpeed + (maxPitchSpeed * 2.0F - minPitchSpeed) * factor;
      } else {
         straightLineYaw = 15.0F;
         straightLinePitch = 5.0F;
         float straightLineReturnPitch = 5.0F;
      }

      if (LegitAura.getInstance().getPitchCorrecting().isValue()) {
         mc.player.setYaw(RotationController.INSTANCE.getRotation().getYaw());
         mc.player.setPitch(RotationController.INSTANCE.getRotation().getPitch());
      }

      if (LegitAura.getInstance().getPitchCorrecting().isValue()) {
         return new Angle(
            currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
            currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
         );
      }

      float currentPlayerPitch = mc.player.getPitch();
      float pitchToPlayerDelta = currentPlayerPitch - currentAngle.getPitch();
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw), mc.player.getPitch() + MathUtils.randomLerp(-1.0F, 1.0F)
      );
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
