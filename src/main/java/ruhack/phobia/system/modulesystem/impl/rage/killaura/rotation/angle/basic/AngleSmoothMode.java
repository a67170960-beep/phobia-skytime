package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic;

import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.util.rotation.angle.Angle;

public abstract class AngleSmoothMode implements IMinecraft {
   private final String name;

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, null, null);
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d) {
      return this.limitAngleChange(currentAngle, targetAngle, vec3d, null);
   }

   public abstract Angle limitAngleChange(Angle var1, Angle var2, Vec3d var3, Entity var4);

   public boolean skipSensitivityAdjust() {
      return false;
   }

   public abstract Vec3d randomValue();

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public AngleSmoothMode(String name) {
      this.name = name;
   }
}
