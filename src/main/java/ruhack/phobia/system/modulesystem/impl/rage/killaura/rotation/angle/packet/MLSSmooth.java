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
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;

public class MLSSmooth extends AngleSmoothMode {
   private static final int POST_HIT_WINDOW = 1;
   private static final int COMBAT_SEQUENCE = 40;
   private final Random random = new Random(5065811L ^ System.nanoTime());
   private float noiseYaw;
   private float noisePitch;
   private float wanderYaw;
   private float wanderPitch;
   private static final float TREMOR_SIGMA = 0.39F;
   private static final float TREMOR_THETA = 0.24F;
   private static final float WANDER_SIGMA = 0.12F;
   private static final float WANDER_THETA = 0.048F;
   private float lastStepYaw;
   private float lastStepPitch;
   private float lastRemYaw = Float.MAX_VALUE;
   private float lastRemPitch;
   private int recoverTicks;
   private int pursueStreak;
   private int postHitTicks;
   private int combatTicks;
   private int rotationTicks;
   private int ticksSinceWindowBreak;
   private int windowBreakTicks;
   private int spikeGuardTicks;
   private int attacksSinceBreak;
   private int lastAttackCount = -1;
   private float lastCloseRatio;
   private int nextWindowBreakAt;
   private float speedDrift;
   private int lockSkewTicks;
   private boolean lockSkewYaw;
   private int lastEntityId = Integer.MIN_VALUE;

   public MLSSmooth() {
      super("MLS");
      this.noiseYaw = this.randGauss();
      this.noisePitch = this.randGauss();
      this.wanderYaw = this.randGauss();
      this.wanderPitch = this.randGauss();
      this.nextWindowBreakAt = 30;
   }

   private void resetState() {
      this.recoverTicks = 0;
      this.pursueStreak = 0;
      this.lastStepYaw = this.lastStepPitch = 0.0F;
      this.lastRemYaw = Float.MAX_VALUE;
      this.lastRemPitch = 0.0F;
      this.combatTicks = 0;
      this.rotationTicks = 0;
      this.ticksSinceWindowBreak = 0;
      this.windowBreakTicks = 0;
      this.spikeGuardTicks = 0;
      this.attacksSinceBreak = 0;
      this.lastCloseRatio = 0.0F;
      this.lockSkewTicks = 0;
      this.lockSkewYaw = this.random.nextBoolean();
      this.nextWindowBreakAt = 26 + this.random.nextInt(14);
      this.speedDrift = 0.0F;
      this.noiseYaw = this.randGauss();
      this.noisePitch = this.randGauss();
      this.wanderYaw = this.randGauss();
      this.wanderPitch = this.randGauss();
   }

   private float strength() {
      return MathHelper.clamp(0.72F + this.speedDrift, 0.64F, 0.82F);
   }

   private void scheduleWindowBreak() {
      this.ticksSinceWindowBreak = 0;
      this.nextWindowBreakAt = 28 + this.random.nextInt(16);
      this.windowBreakTicks = 1;
   }

   private Angle tremorOnly(Angle current, float remYaw, float remPitch, float tremorY, float tremorP) {
      this.pursueStreak = 0;
      this.lastStepYaw = this.lastStepPitch = 0.0F;
      return this.finishStep(current, remYaw, remPitch, tremorY, tremorP, 1.01F);
   }

   private float hitRadius(Entity entity) {
      if (entity != null && mc.player != null) {
         double dist = Math.max(0.5, mc.player.distanceTo(entity));
         float halfW = (float)Math.toDegrees(Math.atan(entity.getWidth() * 0.5 / dist));
         float halfH = (float)Math.toDegrees(Math.atan(entity.getHeight() * 0.5 / dist));
         return MathHelper.clamp(Math.min(halfW, halfH) * 0.77F, 1.0F, 10.0F);
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
         this.attacksSinceBreak++;
         this.postHitTicks = 1;
         this.combatTicks = 0;
         this.pursueStreak = 0;
         this.lastStepYaw = this.lastStepPitch = 0.0F;
         if (this.attacksSinceBreak >= 8) {
            this.attacksSinceBreak = 0;
            this.scheduleWindowBreak();
            this.speedDrift = this.randGauss() * 0.02F;
         }
      }

      if (this.combatTicks < 48) {
         this.combatTicks++;
      }

      this.rotationTicks++;
      this.advanceNoise();
      float strength = this.strength();
      KillAura aura = KillAura.getInstance();
      float aimOffYaw = 0.0F;
      float aimOffPitch = 0.0F;
      if (entity != null && mc.player != null) {
         double dist = Math.max(0.5, mc.player.distanceTo(entity));
         float halfW = (float)Math.toDegrees(Math.atan2(entity.getWidth() * 0.5, dist));
         float halfH = (float)Math.toDegrees(Math.atan2(entity.getHeight() * 0.35, dist));
         aimOffYaw = this.wanderYaw * halfW * 0.38F;
         aimOffPitch = this.wanderPitch * halfH * 0.38F;
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
      float tremorY = this.noiseYaw * this.lerp(0.12F, 0.24F);
      float tremorP = this.noisePitch * this.lerp(0.09F, 0.19F);
      boolean onTarget = entity != null && aura != null && RaytracingUtil.rayTrace(aura.attackDistance(), entity.getBoundingBox());
      float hitRad = this.hitRadius(entity);
      boolean slowZone = onTarget || remaining < 13.0F;
      boolean fastApproach = !onTarget && remaining > 18.0F;
      boolean bridgeZone = !onTarget && remaining >= 13.0F && remaining <= 18.0F;
      if (fastApproach) {
         this.ticksSinceWindowBreak = 0;
      } else if (!onTarget) {
         this.ticksSinceWindowBreak++;
      }

      if (this.recoverTicks > 0) {
         this.recoverTicks--;
         return this.tremorOnly(currentAngle, remYaw, remPitch, tremorY, tremorP);
      }

      if (this.spikeGuardTicks > 0) {
         this.spikeGuardTicks--;
         return this.tremorOnly(currentAngle, remYaw, remPitch, tremorY, tremorP);
      }

      if (!onTarget && !fastApproach && this.windowBreakTicks > 0) {
         this.windowBreakTicks--;
         return this.tremorOnly(currentAngle, remYaw, remPitch, tremorY * 0.92F, tremorP * 0.92F);
      }

      if (!onTarget && !fastApproach && this.ticksSinceWindowBreak >= this.nextWindowBreakAt) {
         this.scheduleWindowBreak();
         return this.tremorOnly(currentAngle, remYaw, remPitch, tremorY, tremorP);
      }

      if (this.postHitTicks > 0 && slowZone) {
         this.postHitTicks--;
         if (this.random.nextFloat() < 0.1F) {
            return this.tremorOnly(currentAngle, remYaw, remPitch, tremorY * 0.9F, tremorP * 0.9F);
         }
      }

      if (this.lastCloseRatio > 0.36F) {
         this.lockSkewTicks = 1;
         this.lockSkewYaw = !this.lockSkewYaw;
      }

      boolean combatWindow = this.combatTicks <= 40;
      float fatigue = this.rotationTicks > 200 ? 0.04F : (this.rotationTicks > 120 ? 0.02F : 0.0F);
      float maxFrac = (combatWindow && this.postHitTicks <= 0 ? 0.9F : 0.96F) - fatigue;
      if (fastApproach) {
         float frac = this.lerp(0.84F, 0.98F) * (0.7F + strength);
         float cap = this.lerp(48.0F, 70.0F) * (0.64F + strength);
         return this.cruise(currentAngle, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, frac, cap, 1.12F, false, false);
      }

      if (bridgeZone) {
         float frac = this.lerp(0.7F, 0.9F) * (0.66F + strength);
         float cap = this.lerp(34.0F, 50.0F) * (0.6F + strength);
         return this.cruise(currentAngle, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, frac, cap, 1.09F, false, true);
      }

      if (onTarget) {
         return this.lockOn(currentAngle, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, strength, maxFrac, hitRad);
      }

      float frac = remaining > 14.0F ? this.lerp(0.58F, 0.82F) * (0.64F + strength) : this.lerp(0.5F, 0.72F) * (0.62F + strength);
      frac = MathHelper.clamp(frac * (0.9F + this.random.nextFloat() * 0.18F), 0.38F, maxFrac);
      float cap = this.lerp(30.0F, 46.0F) * (0.56F + strength);
      return this.cruise(currentAngle, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, frac, cap, 1.08F, true, true);
   }

   @Native
   private Angle cruise(
      Angle current,
      float remYaw,
      float remPitch,
      float remaining,
      float tremorY,
      float tremorP,
      float velYaw,
      float velPitch,
      float frac,
      float softCap,
      float overshoot,
      boolean trackStreak,
      boolean lockMode
   ) {
      if (remaining < 0.001F) {
         return this.finishStep(current, remYaw, remPitch, tremorY, tremorP, 1.02F);
      }

      float targetYaw = remYaw * frac - velYaw * 0.045F;
      float targetPitch = remPitch * frac * this.lerp(0.82F, 0.96F) - velPitch * 0.045F;
      float move = Math.min((float)Math.hypot(targetYaw, targetPitch), softCap);
      if (move > 0.001F) {
         float scale = move / (float)Math.hypot(targetYaw, targetPitch);
         targetYaw *= scale;
         targetPitch *= scale;
      }

      float blend = this.lerp(0.06F, 0.12F);
      float rawYaw = this.lastStepYaw * blend + targetYaw * (1.0F - blend) + tremorY;
      float rawPitch = this.lastStepPitch * blend + targetPitch * (1.0F - blend) + tremorP;
      float wobble = (float)Math.toRadians(this.lerp(-12.0F, 12.0F));
      float wc = (float)Math.cos(wobble);
      float ws = (float)Math.sin(wobble);
      float dirYaw = remYaw / remaining;
      float dirPitch = remPitch / remaining;
      rawYaw += (dirYaw * wc - dirPitch * ws) * this.lerp(0.8F, 2.2F) + this.lerp(-2.0F, 2.0F);
      rawPitch += (dirYaw * ws + dirPitch * wc) * this.lerp(0.6F, 1.8F) + this.lerp(-1.6F, 1.6F);
      return this.commitStep(current, remYaw, remPitch, remaining, rawYaw, rawPitch, tremorY, tremorP, overshoot, trackStreak, lockMode);
   }

   @Native
   private Angle lockOn(
      Angle current,
      float remYaw,
      float remPitch,
      float remaining,
      float tremorY,
      float tremorP,
      float velYaw,
      float velPitch,
      float strength,
      float maxFrac,
      float hitRad
   ) {
      if (remaining < 0.001F) {
         this.pursueStreak = 0;
         return this.finishStep(current, remYaw, remPitch, tremorY, tremorP, 1.02F);
      }

      float yawFrac = MathHelper.clamp(this.lerp(0.52F, 0.72F) * (0.64F + strength), 0.4F, maxFrac);
      float pitchFrac = MathHelper.clamp(this.lerp(0.38F, 0.56F) * (0.6F + strength), 0.3F, maxFrac * 0.88F);
      if (this.lockSkewTicks > 0) {
         this.lockSkewTicks--;
         if (this.lockSkewYaw) {
            pitchFrac *= this.lerp(0.62F, 0.78F);
         } else {
            yawFrac *= this.lerp(0.62F, 0.78F);
         }
      }

      yawFrac *= 0.9F + this.random.nextFloat() * 0.16F;
      pitchFrac *= 0.9F + this.random.nextFloat() * 0.14F;
      float rawYaw = remYaw * yawFrac - velYaw * 0.05F + tremorY + this.wanderYaw * this.lerp(0.08F, 0.14F);
      float rawPitch = remPitch * pitchFrac - velPitch * 0.05F + tremorP + this.wanderPitch * this.lerp(0.06F, 0.12F);
      float blend = this.lerp(0.1F, 0.18F);
      rawYaw = this.lastStepYaw * blend + rawYaw * (1.0F - blend);
      rawPitch = this.lastStepPitch * blend + rawPitch * (1.0F - blend);
      float yawCap = this.lerp(26.0F, 40.0F) * (0.58F + strength);
      float pitchCap = this.lerp(18.0F, 28.0F) * (0.54F + strength);
      float yMag = Math.abs(rawYaw);
      float pMag = Math.abs(rawPitch);
      if (yMag > yawCap) {
         rawYaw *= yawCap / yMag;
      }

      if (pMag > pitchCap) {
         rawPitch *= pitchCap / pMag;
      }

      return this.commitStep(current, remYaw, remPitch, remaining, rawYaw, rawPitch, tremorY, tremorP, 1.06F, true, true);
   }

   @Native
   private Angle commitStep(
      Angle current,
      float remYaw,
      float remPitch,
      float remaining,
      float rawYaw,
      float rawPitch,
      float tremorY,
      float tremorP,
      float overshoot,
      boolean trackStreak,
      boolean lockMode
   ) {
      float stepYaw = this.quantizeDelta(rawYaw);
      float stepPitch = this.quantizeDelta(rawPitch);
      stepYaw = MathHelper.clamp(stepYaw, lockMode ? -36.0F : -42.0F, lockMode ? 36.0F : 42.0F);
      stepPitch = MathHelper.clamp(stepPitch, lockMode ? -26.0F : -30.0F, lockMode ? 26.0F : 30.0F);
      if (lockMode) {
         float yClose = Math.abs(stepYaw) / (Math.abs(remYaw) + 0.001F);
         float pClose = Math.abs(stepPitch) / (Math.abs(remPitch) + 0.001F);
         if (yClose > 0.78F) {
            stepYaw = this.quantizeDelta(stepYaw * this.lerp(0.74F, 0.86F));
         }

         if (pClose > 0.78F) {
            stepPitch = this.quantizeDelta(stepPitch * this.lerp(0.74F, 0.86F));
         }
      }

      float errSq = remaining * remaining + 0.001F;
      float closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      float stepMag = (float)Math.hypot(stepYaw, stepPitch);
      if (lockMode && remaining < 18.0F && closeRatio > 0.24F) {
         float[] peeled = this.deflectStep(remYaw, remPitch, remaining, closeRatio, stepYaw, stepPitch);
         stepYaw = this.quantizeDelta(peeled[0]);
         stepPitch = this.quantizeDelta(peeled[1]);
         stepMag = (float)Math.hypot(stepYaw, stepPitch);
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      }

      if (lockMode && remaining < 15.0F && closeRatio > 0.36F) {
         float[] peeled = this.deflectStep(remYaw, remPitch, remaining, closeRatio, stepYaw, stepPitch);
         stepYaw = this.quantizeDelta(peeled[0] * this.lerp(0.84F, 0.94F));
         stepPitch = this.quantizeDelta(peeled[1] * this.lerp(0.84F, 0.94F));
         stepMag = (float)Math.hypot(stepYaw, stepPitch);
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      }

      if (lockMode && remaining < 14.0F && closeRatio > 0.42F) {
         this.lockSkewTicks = 1;
         this.lockSkewYaw = !this.lockSkewYaw;
         this.lastCloseRatio = 0.0F;
         this.lastStepYaw = this.lastStepPitch = 0.0F;
         float dirYaw = remYaw / (remaining + 0.001F);
         float dirPitch = remPitch / (remaining + 0.001F);
         stepYaw = this.quantizeDelta(-dirPitch * this.lerp(1.2F, 2.8F) + tremorY);
         stepPitch = this.quantizeDelta(dirYaw * this.lerp(0.9F, 2.2F) + tremorP);
         stepMag = (float)Math.hypot(stepYaw, stepPitch);
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      }

      if (!lockMode && remaining < 16.0F && closeRatio > 0.42F) {
         this.spikeGuardTicks = 1;
         this.lastCloseRatio = 0.0F;
         return this.tremorOnly(current, remYaw, remPitch, tremorY, tremorP);
      }

      this.lastStepYaw = stepYaw;
      this.lastStepPitch = stepPitch;
      this.lastCloseRatio = closeRatio;
      if (trackStreak) {
         this.pursueStreak++;
         if (lockMode && closeRatio > 0.48F && remaining < 8.0F) {
            this.recoverTicks = 1;
         } else if (stepMag > 16.0F) {
            this.recoverTicks = 1;
         } else if (this.pursueStreak >= 4 + this.random.nextInt(2)) {
            this.recoverTicks = 1;
         }
      }

      float outMul = lockMode ? Math.min(overshoot, 1.06F) : overshoot;
      return this.finishStep(current, remYaw, remPitch, stepYaw, stepPitch, outMul);
   }

   private float[] deflectStep(float remYaw, float remPitch, float remaining, float closeRatio, float stepYaw, float stepPitch) {
      float dirYaw = remYaw / (remaining + 0.001F);
      float dirPitch = remPitch / (remaining + 0.001F);
      float mag = (float)Math.hypot(stepYaw, stepPitch);
      float peel = this.lerp(0.1F, 0.24F) * MathHelper.clamp((closeRatio - 0.24F) / 0.14F, 0.0F, 1.0F);
      float outYaw = stepYaw - dirYaw * mag * peel - dirPitch * this.lerp(0.4F, 1.2F);
      float outPitch = stepPitch - dirPitch * mag * peel + dirYaw * this.lerp(0.3F, 1.0F) * 0.85F;
      return new float[]{outYaw, outPitch};
   }

   private Angle finishStep(Angle current, float remYaw, float remPitch, float stepYaw, float stepPitch, float overshoot) {
      float outYaw = current.getYaw() + clampOvershoot(stepYaw, remYaw, overshoot);
      float outPitch = MathHelper.clamp(current.getPitch() + clampOvershoot(stepPitch, remPitch, overshoot), -90.0F, 90.0F);
      return new Angle(outYaw, outPitch);
   }

   private float quantizeDelta(float delta) {
      double gcd = MathUtils.computeGcd();
      if (gcd <= 1.0E-6) {
         return delta;
      }

      long steps = Math.round(delta / gcd);
      if (steps == 0L && Math.abs(delta) > gcd * 0.28F) {
         steps = delta > 0.0F ? 1L : -1L;
      }

      if (Math.abs(delta) > gcd * 2.2F && Math.abs(steps) < 2L) {
         steps = delta > 0.0F ? 2L : -2L;
      }

      if (Math.abs(delta) > gcd * 5.0 && Math.abs(steps) < 3L) {
         steps = delta > 0.0F ? 3L : -3L;
      }

      if (Math.abs(delta) > gcd * 9.0 && Math.abs(steps) < 4L) {
         steps = delta > 0.0F ? 4L : -4L;
      }

      return (float)(steps * gcd);
   }

   private static float clampOvershoot(float step, float remaining, float mul) {
      float r = Math.abs(remaining);
      return MathHelper.clamp(step, -r * mul, r * mul);
   }

   private void advanceNoise() {
      this.noiseYaw = this.noiseYaw + (-0.24F * this.noiseYaw + 0.39F * this.randGauss());
      this.noisePitch = this.noisePitch + (-0.24F * this.noisePitch + 0.39F * this.randGauss());
      this.wanderYaw = this.wanderYaw + (-0.048F * this.wanderYaw + 0.12F * this.randGauss());
      this.wanderPitch = this.wanderPitch + (-0.048F * this.wanderPitch + 0.12F * this.randGauss());
      this.speedDrift = this.speedDrift + (-0.06F * this.speedDrift + 0.035F * this.randGauss());
      this.speedDrift = MathHelper.clamp(this.speedDrift, -0.09F, 0.09F);
   }

   private float randGauss() {
      return (float)this.random.nextGaussian();
   }

   private float lerp(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }

   @Override
   public Vec3d randomValue() {
      double s = 0.0017;
      return new Vec3d((this.random.nextDouble() * 2.0 - 1.0) * s, (this.random.nextDouble() * 2.0 - 1.0) * s, (this.random.nextDouble() * 2.0 - 1.0) * s);
   }
}
