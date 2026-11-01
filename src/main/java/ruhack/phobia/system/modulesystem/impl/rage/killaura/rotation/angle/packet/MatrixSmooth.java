package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

import com.adl.nativeprotect.Native;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;

public class MatrixSmooth extends AngleSmoothMode {
   public MatrixSmooth() {
      super("Matrix");
   }

   @Native
   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      boolean shouldAttack = entity != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 2);
      boolean shouldAttack2 = entity != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 0);
      boolean shouldAttack3 = entity != null
         && Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().canAttack(KillAura.getInstance().getConfig(), 5);
      boolean rayTrace = entity != null && RaytracingUtil.rayTrace(KillAura.getInstance().attackDistance(), entity.getBoundingBox());
      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (shouldAttack2 ? 100.0F : MathUtils.randomLerp(25.0F, 50.0F));
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (shouldAttack2 ? 100.0F : MathUtils.randomLerp(5.0F, 25.0F));
      if (!Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler().getAttackTimer().finished(MathUtils.randomLerp(0.0F, 300.0F))) {
         straightLinePitch = -4.0F;
         straightLineYaw = -4.0F;
      }

      float jittetY = shouldAttack3 ? (!rayTrace && shouldAttack ? 0.0F : MathUtils.randomLerp(3.0F, -3.0F)) : 0.0F;
      float jittetX = shouldAttack3 ? (!rayTrace && shouldAttack ? 0.0F : MathUtils.randomLerp(3.0F, -3.0F)) : 0.0F;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw) + jittetX,
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch) + jittetY
      );
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }
}
