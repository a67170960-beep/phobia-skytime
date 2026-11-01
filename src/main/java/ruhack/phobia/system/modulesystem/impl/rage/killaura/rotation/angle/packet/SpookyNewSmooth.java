package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

import com.adl.nativeprotect.Native;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.impl.rage.KillAura;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackHandler;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.util.aim.neuro.NeuroModel;
import ruhack.phobia.util.aim.neuro.SpookyNewModel;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;

public class SpookyNewSmooth extends AngleSmoothMode {
   private final Random random = new Random(356986263559L ^ System.nanoTime());
   private final NeuroModel model = SpookyNewModel.get();
   private float noiseYaw;
   private float noisePitch;
   private float wanderYaw;
   private float wanderPitch;
   private static final float TREMOR_SIGMA = 0.4F;
   private static final float TREMOR_THETA = 0.25F;
   private static final float WANDER_SIGMA = 0.13F;
   private static final float WANDER_THETA = 0.05F;
   private float speedDrift;
   private float lastRemYaw = Float.MAX_VALUE;
   private float lastRemPitch;
   private float lastHitRad = 2.0F;
   private int lockTicks;
   private int glanceTicks;
   private float glanceYaw;
   private float glancePitch;
   private int recoverTicks;
   private int pursueStreak;
   private int postHitTicks;
   private int pauseTicks;
   private int onTargetTicks;
   private int nextPauseAt;
   private int attacksSinceWander;
   private int lastAttackCount = -1;
   private int lastEntityId = Integer.MIN_VALUE;

   public SpookyNewSmooth() {
      super("SpookyNew");
      this.noiseYaw = this.randGauss();
      this.noisePitch = this.randGauss();
      this.wanderYaw = this.randGauss();
      this.wanderPitch = this.randGauss();
      this.nextPauseAt = 46;
   }

   private void resetState() {
      this.lockTicks = 0;
      this.glanceTicks = 0;
      this.glanceYaw = this.glancePitch = 0.0F;
      this.recoverTicks = 0;
      this.pursueStreak = 0;
      this.pauseTicks = 0;
      this.onTargetTicks = 0;
      this.attacksSinceWander = 0;
      this.speedDrift = 0.0F;
      this.nextPauseAt = 42 + this.random.nextInt(16);
      this.lastRemYaw = Float.MAX_VALUE;
      this.lastRemPitch = 0.0F;
      this.noiseYaw = this.randGauss();
      this.noisePitch = this.randGauss();
      this.wanderYaw = this.randGauss();
      this.wanderPitch = this.randGauss();
      this.model.resetPlayback();
   }

   private float strength() {
      return MathHelper.clamp(0.5F + this.speedDrift, 0.46F, 0.56F);
   }

   private float hitRadius(Entity entity) {
      if (entity != null && mc.player != null) {
         double dist = Math.max(0.5, mc.player.distanceTo(entity));
         float halfW = (float)Math.toDegrees(Math.atan(entity.getWidth() * 0.5 / dist));
         float halfH = (float)Math.toDegrees(Math.atan(entity.getHeight() * 0.5 / dist));
         return MathHelper.clamp(Math.min(halfW, halfH) * 0.76F, 1.0F, 10.0F);
      } else {
         return 2.0F;
      }
   }

   @Native
   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      int eid = entity == null ? Integer.MIN_VALUE : entity.getId();
      if (eid != this.lastEntityId) {
         this.resetState();
         this.postHitTicks = 0;
         this.lastEntityId = eid;
      }

      AttackHandler handler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
      int attackCount = handler.getCount();
      if (attackCount != this.lastAttackCount) {
         this.lastAttackCount = attackCount;
         this.postHitTicks = 2;
         this.pursueStreak = 0;
         this.pauseTicks = 1;
         this.attacksSinceWander++;
         if (this.attacksSinceWander >= 7 + this.random.nextInt(2)) {
            this.attacksSinceWander = 0;
            this.wanderYaw = this.randGauss() * 0.8F;
            this.wanderPitch = this.randGauss() * 0.8F;
         }
      }

      this.advanceNoise();
      float strength = this.strength();
      KillAura aura = KillAura.getInstance();
      float aimOffYaw = 0.0F;
      float aimOffPitch = 0.0F;
      if (entity != null && mc.player != null) {
         double dist = Math.max(0.5, mc.player.distanceTo(entity));
         float halfW = (float)Math.toDegrees(Math.atan2(entity.getWidth() * 0.5, dist));
         float halfH = (float)Math.toDegrees(Math.atan2(entity.getHeight() * 0.35, dist));
         float offMul = dist > 4.0 ? 0.46F : (dist > 2.5 ? 0.34F : 0.26F);
         aimOffYaw = this.wanderYaw * halfW * offMul;
         aimOffPitch = this.wanderPitch * halfH * offMul;
      }

      Angle aim = new Angle(targetAngle.getYaw() + aimOffYaw, targetAngle.getPitch() + aimOffPitch);
      Angle delta = AngleUtil.calculateDelta(currentAngle, aim);
      float remYaw = delta.getYaw();
      float remPitch = delta.getPitch();
      float remaining = (float)Math.hypot(remYaw, remPitch);
      float velYaw = 0.0F;
      float velPitch = 0.0F;
      if (this.lastRemYaw != Float.MAX_VALUE) {
         velYaw = remYaw - this.lastRemYaw;
         velPitch = remPitch - this.lastRemPitch;
      }

      this.lastRemYaw = remYaw;
      this.lastRemPitch = remPitch;
      float tremorY = this.noiseYaw * this.lerp(0.14F, 0.32F);
      float tremorP = this.noisePitch * this.lerp(0.11F, 0.26F);
      boolean onTarget = entity != null && aura != null && RaytracingUtil.rayTrace(aura.attackDistance(), entity.getBoundingBox());
      float hitRad = this.hitRadius(entity);
      this.lastHitRad = hitRad;
      boolean fastApproach = !onTarget && remaining > 18.0F;
      boolean bridgeZone = !onTarget && remaining >= 13.0F && remaining <= 18.0F;
      if (onTarget) {
         this.onTargetTicks++;
         if (this.onTargetTicks >= this.nextPauseAt) {
            this.onTargetTicks = 0;
            this.nextPauseAt = 40 + this.random.nextInt(18);
            this.pauseTicks = 1;
         }
      } else {
         this.onTargetTicks = 0;
      }

      if (this.pauseTicks > 0 && onTarget) {
         this.pauseTicks--;
         this.pursueStreak = 0;
         return this.tremorStep(currentAngle, remYaw, remPitch, tremorY, tremorP);
      }

      if (this.recoverTicks > 0 && onTarget) {
         this.recoverTicks--;
         this.pursueStreak = 0;
         return this.tremorStep(currentAngle, remYaw, remPitch, tremorY * 0.85F, tremorP * 0.85F);
      }

      if (this.postHitTicks > 0 && onTarget) {
         int left = this.postHitTicks--;
         if (left >= 2 && this.random.nextFloat() < 0.34F) {
            this.pursueStreak = 0;
            return this.tremorStep(currentAngle, remYaw, remPitch, tremorY * 0.78F, tremorP * 0.78F);
         }

         if (left == 1 && this.random.nextFloat() < 0.22F) {
            this.pursueStreak = 0;
            return this.tremorStep(currentAngle, remYaw, remPitch, tremorY * 0.7F, tremorP * 0.7F);
         }
      }

      if (onTarget) {
         this.lockTicks++;
         if (this.lockTicks > 10 + this.random.nextInt(10) && this.glanceTicks <= 0) {
            this.glanceTicks = 1;
            this.lockTicks = 0;
            this.pursueStreak = 0;
            float ang = this.random.nextFloat() * (float) (Math.PI * 2);
            float mag = this.lerp(1.8F, 4.6F);
            this.glanceYaw = (float)Math.cos(ang) * mag;
            this.glancePitch = (float)Math.sin(ang) * mag * 0.48F;
         }
      } else {
         this.lockTicks = 0;
         this.pursueStreak = 0;
      }

      if (this.glanceTicks > 0) {
         this.glanceTicks--;
         this.pursueStreak = 0;
         float k = this.lerp(0.68F, 1.0F);
         return this.applyStep(currentAngle, remYaw, remPitch, this.glanceYaw * k + tremorY, this.glancePitch * k + tremorP, 1.05F);
      }

      if (fastApproach) {
         return this.pursue(
            currentAngle,
            remYaw,
            remPitch,
            remaining,
            strength,
            tremorY,
            tremorP,
            this.lerp(0.76F, 0.9F) * (0.62F + strength),
            this.lerp(36.0F, 50.0F) * (0.56F + strength),
            1.1F,
            velYaw,
            velPitch,
            false,
            38.0F,
            32.0F,
            false
         );
      }

      if (bridgeZone) {
         return this.pursue(
            currentAngle,
            remYaw,
            remPitch,
            remaining,
            strength,
            tremorY,
            tremorP,
            this.lerp(0.58F, 0.76F) * (0.58F + strength),
            this.lerp(26.0F, 38.0F) * (0.52F + strength),
            1.08F,
            velYaw,
            velPitch,
            false,
            30.0F,
            26.0F,
            false
         );
      }

      float longFightTremor = this.onTargetTicks > 50 ? 0.06F : 0.0F;
      if (onTarget && remaining < hitRad * 1.45F) {
         this.pursueStreak = 0;
         return this.random.nextFloat() < 0.74F + longFightTremor
            ? this.tremorStep(currentAngle, remYaw, remPitch, tremorY, tremorP)
            : this.pursue(
               currentAngle,
               remYaw,
               remPitch,
               remaining,
               strength,
               tremorY,
               tremorP,
               this.lerp(0.14F, 0.26F),
               this.lerp(10.0F, 17.0F),
               1.03F,
               velYaw,
               velPitch,
               false,
               18.0F,
               15.0F,
               true
            );
      }

      if (onTarget) {
         if (this.pursueStreak >= 2) {
            this.pauseTicks = 1;
            this.pursueStreak = 0;
            return this.tremorStep(currentAngle, remYaw, remPitch, tremorY, tremorP);
         } else {
            return this.random.nextFloat() < 0.52F + longFightTremor
               ? this.tremorStep(currentAngle, remYaw, remPitch, tremorY, tremorP)
               : this.pursue(
                  currentAngle,
                  remYaw,
                  remPitch,
                  remaining,
                  strength,
                  tremorY,
                  tremorP,
                  this.lerp(0.18F, 0.32F),
                  this.lerp(14.0F, 22.0F),
                  1.04F,
                  velYaw,
                  velPitch,
                  true,
                  22.0F,
                  18.0F,
                  true
               );
         }
      } else {
         if (!onTarget && remaining < hitRad * 2.6F) {
            return this.pursue(
               currentAngle,
               remYaw,
               remPitch,
               remaining,
               strength,
               tremorY,
               tremorP,
               this.lerp(0.5F, 0.72F),
               this.lerp(20.0F, 30.0F),
               1.06F,
               velYaw,
               velPitch,
               false,
               34.0F,
               28.0F,
               false
            );
         }

         float frac = remaining > 14.0F ? this.lerp(0.54F, 0.76F) * (0.56F + strength) : this.lerp(0.42F, 0.64F) * (0.52F + strength);
         frac = MathHelper.clamp(frac, 0.3F, 0.82F);
         float softCap = this.lerp(24.0F, 38.0F) * (0.5F + strength);
         return this.pursue(
            currentAngle,
            remYaw,
            remPitch,
            remaining,
            strength,
            tremorY,
            tremorP,
            frac,
            softCap,
            remaining > 10.0F ? 1.07F : 1.04F,
            velYaw,
            velPitch,
            false,
            32.0F,
            26.0F,
            false
         );
      }
   }

   private Angle pursue(
      Angle current,
      float remYaw,
      float remPitch,
      float remaining,
      float strength,
      float tremorY,
      float tremorP,
      float frac,
      float softCap,
      float overshoot,
      float velYaw,
      float velPitch,
      boolean trackStreak,
      float yawCap,
      float pitchCap,
      boolean splitAxis
   ) {
      if (remaining < 0.001F) {
         this.pursueStreak = 0;
         return this.tremorStep(current, remYaw, remPitch, tremorY, tremorP);
      }

      float move = Math.min(remaining * frac * overshoot, softCap);
      move = Math.max(move, 0.55F);
      float dirYaw = remYaw / remaining;
      float dirPitch = remPitch / remaining;
      float wobble = (float)Math.toRadians(this.lerp(-16.0F, 16.0F));
      float wc = (float)Math.cos(wobble);
      float ws = (float)Math.sin(wobble);
      float wDirYaw = dirYaw * wc - dirPitch * ws;
      float wDirPitch = dirYaw * ws + dirPitch * wc;
      float orthoY = this.lerp(-3.2F, 3.2F);
      float orthoP = this.lerp(-2.6F, 2.6F);
      float useMove = move;
      if (SpookyNewModel.isReady()) {
         float[] raw = this.model.next(remYaw, remPitch, this.lastHitRad);
         if (raw != null) {
            float rawMag = (float)Math.hypot(raw[0], raw[1]);
            if (rawMag > 0.001F) {
               float errLenSq = remaining * remaining + 0.001F;
               float closeFrac = MathHelper.clamp((raw[0] * remYaw + raw[1] * remPitch) / errLenSq, -1.0F, 1.0F);
               float magBlend = MathHelper.lerp(0.58F, rawMag, move);
               useMove = MathHelper.clamp(magBlend, move * 0.74F, move * 1.06F);
               if (closeFrac < 0.12F) {
                  useMove = move;
               }
            }
         }
      }

      float stepYaw = wDirYaw * useMove - velYaw * 0.06F + tremorY + orthoY + this.wanderYaw * this.lerp(0.1F, 0.18F);
      float stepPitch = wDirPitch * useMove * this.lerp(0.76F, 0.94F) - velPitch * 0.06F + tremorP + orthoP + this.wanderPitch * this.lerp(0.08F, 0.14F);
      if (splitAxis && this.random.nextInt(5) == 0) {
         if (this.random.nextBoolean()) {
            stepPitch = tremorP + orthoP + this.wanderPitch * 0.09F;
         } else {
            stepYaw = tremorY + orthoY + this.wanderYaw * 0.11F;
         }
      } else if (!trackStreak && this.random.nextInt(7) == 0) {
         if (this.random.nextBoolean()) {
            stepPitch *= this.lerp(0.55F, 0.75F);
         } else {
            stepYaw *= this.lerp(0.55F, 0.75F);
         }
      }

      if (trackStreak && this.random.nextFloat() < 0.14F) {
         stepYaw -= wDirYaw * this.lerp(1.2F, 3.0F);
         stepPitch -= wDirPitch * this.lerp(0.8F, 2.4F);
      }

      stepYaw = MathHelper.clamp(stepYaw, -yawCap, yawCap);
      stepPitch = MathHelper.clamp(stepPitch, -pitchCap, pitchCap);
      float mag = (float)Math.hypot(stepYaw, stepPitch);
      if (trackStreak) {
         this.pursueStreak++;
         if (mag > 10.0F) {
            this.recoverTicks = 1;
         } else if (this.pursueStreak >= 2) {
            this.pauseTicks = 1;
            this.pursueStreak = 0;
         }
      }

      return this.applyStep(current, remYaw, remPitch, stepYaw, stepPitch, overshoot);
   }

   private Angle tremorStep(Angle current, float remYaw, float remPitch, float tremorY, float tremorP) {
      return this.applyStep(
         current, remYaw, remPitch, tremorY + this.wanderYaw * this.lerp(0.12F, 0.24F), tremorP + this.wanderPitch * this.lerp(0.09F, 0.18F), 1.03F
      );
   }

   private void advanceNoise() {
      this.noiseYaw = this.noiseYaw + (-0.25F * this.noiseYaw + 0.4F * this.randGauss());
      this.noisePitch = this.noisePitch + (-0.25F * this.noisePitch + 0.4F * this.randGauss());
      this.wanderYaw = this.wanderYaw + (-0.05F * this.wanderYaw + 0.13F * this.randGauss());
      this.wanderPitch = this.wanderPitch + (-0.05F * this.wanderPitch + 0.13F * this.randGauss());
      this.speedDrift = this.speedDrift + (-0.06F * this.speedDrift + 0.028F * this.randGauss());
      this.speedDrift = MathHelper.clamp(this.speedDrift, -0.06F, 0.06F);
   }

   private static float clampOvershoot(float step, float remaining, float mul) {
      float r = Math.abs(remaining);
      return MathHelper.clamp(step, -r * mul, r * mul);
   }

   private Angle applyStep(Angle current, float remYaw, float remPitch, float stepYaw, float stepPitch, float mul) {
      return new Angle(
         current.getYaw() + clampOvershoot(stepYaw, remYaw, mul),
         MathHelper.clamp(current.getPitch() + clampOvershoot(stepPitch, remPitch, mul), -90.0F, 90.0F)
      );
   }

   private float randGauss() {
      return (float)this.random.nextGaussian();
   }

   private float lerp(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }

   @Override
   public Vec3d randomValue() {
      double s = 0.002;
      return new Vec3d((this.random.nextDouble() * 2.0 - 1.0) * s, (this.random.nextDouble() * 2.0 - 1.0) * s, (this.random.nextDouble() * 2.0 - 1.0) * s);
   }
}
