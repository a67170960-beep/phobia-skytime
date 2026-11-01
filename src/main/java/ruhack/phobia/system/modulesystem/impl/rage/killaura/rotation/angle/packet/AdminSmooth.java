package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class AdminSmooth extends AngleSmoothMode {
   public AdminSmooth() {
      super("Admin");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (entity != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 0)) {
         boolean canAttack = true;
      } else {
         boolean canAttack = false;
      }

      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 45.0F;
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 25.0F;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + MathUtils.randomLerp(-1.0F, 1.0F),
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + MathUtils.randomLerp(-1.0F, 1.0F)
      );
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
