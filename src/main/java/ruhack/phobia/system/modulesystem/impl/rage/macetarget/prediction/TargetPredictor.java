package ruhack.phobia.system.modulesystem.impl.rage.macetarget.prediction;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.modulesystem.impl.rage.macetarget.state.MaceState;

public class TargetPredictor {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private Vec3d lastPosition = null;
   private Vec3d velocity = Vec3d.ZERO;
   private Vec3d smoothedVelocity = Vec3d.ZERO;
   private long lastUpdateTime = 0L;
   private int sampleCount = 0;
   private static final int MIN_SAMPLES = 2;
   private static final double SMOOTHING = 0.6;

   public void update(LivingEntity target) {
      if (target == null) {
         this.reset();
      } else {
         Vec3d currentPos = target.getEntityPos();
         long currentTime = System.currentTimeMillis();
         if (this.lastPosition != null && this.lastUpdateTime > 0L) {
            long deltaTime = currentTime - this.lastUpdateTime;
            if (deltaTime > 0L && deltaTime < 500L) {
               this.velocity = currentPos.subtract(this.lastPosition);
               this.smoothedVelocity = this.smoothedVelocity.multiply(0.6).add(this.velocity.multiply(0.4));
               this.sampleCount++;
            }
         }

         this.lastPosition = currentPos;
         this.lastUpdateTime = currentTime;
      }
   }

   public Vec3d getPredictedPosition(LivingEntity target, MaceState.Stage stage) {
      if (target != null && mc.player != null) {
         Vec3d currentPos = target.getEntityPos();
         if (this.sampleCount >= 2 && !(this.smoothedVelocity.horizontalLengthSquared() < 1.0E-4)) {
            double distance = mc.player.distanceTo(target);
            double playerSpeed = mc.player.getVelocity().length();
            double targetSpeed = this.smoothedVelocity.horizontalLength();

            double var15 = Math.min(switch (stage) {
               case FLYING_UP -> {
                  double heightDiff = Math.abs(mc.player.getY() - target.getY());
                  yield (heightDiff + distance) / Math.max(playerSpeed * 20.0, 1.0) * 1.2;
               }
               case TARGETTING -> distance / Math.max(playerSpeed * 20.0, 0.8);
               case ATTACKING -> distance / Math.max(playerSpeed * 20.0, 1.5) * 0.8;
               default -> distance / 2.0;
            }, 40.0);
            double leadMultiplier = 1.0;
            if (targetSpeed > 0.2) {
               leadMultiplier = 1.3;
            }

            if (targetSpeed > 0.4) {
               leadMultiplier = 1.5;
            }

            Vec3d prediction = this.smoothedVelocity.multiply(var15 * leadMultiplier);
            return currentPos.add(prediction);
         } else {
            return currentPos;
         }
      } else {
         return Vec3d.ZERO;
      }
   }

   public boolean isMoving() {
      return this.smoothedVelocity.horizontalLengthSquared() > 0.001;
   }

   public void reset() {
      this.lastPosition = null;
      this.velocity = Vec3d.ZERO;
      this.smoothedVelocity = Vec3d.ZERO;
      this.lastUpdateTime = 0L;
      this.sampleCount = 0;
   }

   @Generated
   public Vec3d getLastPosition() {
      return this.lastPosition;
   }

   @Generated
   public Vec3d getVelocity() {
      return this.velocity;
   }

   @Generated
   public Vec3d getSmoothedVelocity() {
      return this.smoothedVelocity;
   }

   @Generated
   public long getLastUpdateTime() {
      return this.lastUpdateTime;
   }

   @Generated
   public int getSampleCount() {
      return this.sampleCount;
   }
}
