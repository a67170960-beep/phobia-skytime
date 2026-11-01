package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

import com.adl.nativeprotect.Native;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class Snap extends AngleSmoothMode {
   public Snap() {
      super("Intave");
   }

   @Native
   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (entity != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 1)) {
         Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         if (entity != null
            && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 0)) {
            boolean canAttack = true;
         } else {
            boolean canAttack = false;
         }

         float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
         float straightLineYaw = Math.abs(yawDelta / rotationDifference) * KillAura.getInstance().getSpeedValue().getValue() * 1.5F;
         float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * KillAura.getInstance().getSpeedValue().getValue() * 1.5F;
         return new Angle(
            currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
            currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
         );
      } else {
         Angle playerViewAngle = new Angle(mc.player.getYaw(), mc.player.getPitch());
         Angle angleToPlayerView = AngleUtil.calculateDelta(currentAngle, playerViewAngle);
         float yawDelta = angleToPlayerView.getYaw();
         float pitchDelta = angleToPlayerView.getPitch();
         float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
         float straightLineYaw = Math.abs(yawDelta / rotationDifference)
            * (
               !Initialization.getInstance()
                     .getManager()
                     .getAttackPerpetrator()
                     .getAttackHandler()
                     .getAttackTimer()
                     .finished(KillAura.getInstance().getTimeOnTick().getValue())
                  ? 0.0F
                  : KillAura.getInstance().getSpeedValue().getValue()
            );
         float straightLinePitch = Math.abs(pitchDelta / rotationDifference)
            * (
               !Initialization.getInstance()
                     .getManager()
                     .getAttackPerpetrator()
                     .getAttackHandler()
                     .getAttackTimer()
                     .finished(KillAura.getInstance().getTimeOnTick().getValue())
                  ? 0.0F
                  : KillAura.getInstance().getSpeedValue().getValue()
            );
         return new Angle(
            currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
            currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
         );
      }
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
