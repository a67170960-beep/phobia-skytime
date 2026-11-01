package ruhack.phobia.util.rotation;

import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class RotationPlan implements IMinecraft {
   private final Angle angle;
   private final Vec3d vec3d;
   private final Entity entity;
   private final AngleSmoothMode angleSmooth;
   private final int ticksUntilReset;
   private final float resetThreshold;
   private final boolean moveCorrection;
   private final boolean freeCorrection;

   public Angle nextRotation(Angle fromAngle, boolean isResetting) {
      return isResetting
         ? this.angleSmooth.limitAngleChange(fromAngle, AngleUtil.cameraAngle())
         : this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
   }

   @Generated
   public Angle getAngle() {
      return this.angle;
   }

   @Generated
   public Vec3d getVec3d() {
      return this.vec3d;
   }

   @Generated
   public Entity getEntity() {
      return this.entity;
   }

   @Generated
   public AngleSmoothMode getAngleSmooth() {
      return this.angleSmooth;
   }

   @Generated
   public int getTicksUntilReset() {
      return this.ticksUntilReset;
   }

   @Generated
   public float getResetThreshold() {
      return this.resetThreshold;
   }

   @Generated
   public boolean isMoveCorrection() {
      return this.moveCorrection;
   }

   @Generated
   public boolean isFreeCorrection() {
      return this.freeCorrection;
   }

   @Generated
   public RotationPlan(
      Angle angle,
      Vec3d vec3d,
      Entity entity,
      AngleSmoothMode angleSmooth,
      int ticksUntilReset,
      float resetThreshold,
      boolean moveCorrection,
      boolean freeCorrection
   ) {
      this.angle = angle;
      this.vec3d = vec3d;
      this.entity = entity;
      this.angleSmooth = angleSmooth;
      this.ticksUntilReset = ticksUntilReset;
      this.resetThreshold = resetThreshold;
      this.moveCorrection = moveCorrection;
      this.freeCorrection = freeCorrection;
   }
}
