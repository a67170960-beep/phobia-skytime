package ruhack.phobia.util.rotation.angle;

import lombok.Generated;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.RotationController;

public class Angle {
   public static Angle DEFAULT = new Angle(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Angle adjustSensitivity() {
      double gcd = MathUtils.computeGcd();
      Angle previousAngle = RotationController.INSTANCE.getServerAngle();
      float adjustedYaw = this.adjustAxis(this.yaw, previousAngle.yaw, gcd);
      float adjustedPitch = this.adjustAxis(this.pitch, previousAngle.pitch, gcd);
      return new Angle(adjustedYaw, MathHelper.clamp(adjustedPitch, -90.0F, 90.0F));
   }

   public Angle random(float f) {
      return new Angle(this.yaw + MathUtils.getRandom(-f, f), this.pitch + MathUtils.getRandom(-f, f));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd) {
      float delta = axisValue - previousValue;
      return previousValue + (float)Math.round(delta / gcd) * (float)gcd;
   }

   public final Vec3d toVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float g = -this.yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public Angle addYaw(float yaw) {
      return new Angle(this.yaw + yaw, this.pitch);
   }

   public Angle addPitch(float pitch) {
      this.pitch = MathHelper.clamp(this.pitch + pitch, -90.0F, 90.0F);
      return this;
   }

   public Angle of(Angle angle) {
      return new Angle(angle.getYaw(), angle.getPitch());
   }

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
   }

   @Generated
   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Generated
   @Override
   public String toString() {
      return "Angle(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   @Generated
   public Angle(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public static class VecRotation {
      private final Angle angle;
      private final Vec3d vec;

      @Generated
      @Override
      public String toString() {
         return "Angle.VecRotation(angle=" + this.getAngle() + ", vec=" + this.getVec() + ")";
      }

      @Generated
      public Angle getAngle() {
         return this.angle;
      }

      @Generated
      public Vec3d getVec() {
         return this.vec;
      }

      @Generated
      public VecRotation(Angle angle, Vec3d vec) {
         this.angle = angle;
         this.vec = vec;
      }
   }
}
