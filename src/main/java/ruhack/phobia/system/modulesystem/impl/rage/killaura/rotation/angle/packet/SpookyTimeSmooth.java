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
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;

public class SpookyTimeSmooth extends AngleSmoothMode {
   private static final int POST_HIT_WINDOW = 1;
   private static final int COMBAT_SEQUENCE = 40;
   private final Random random = new Random(357829660491L ^ System.nanoTime());
   private final NeuroModel model = SpookyNewModel.get();
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
   private float lastCloseRatio;
   private float speedDrift;
   private int recoverTicks;
   private int pursueStreak;
   private int postHitTicks;
   private int combatTicks;
   private int rotationTicks;
   private int spikeGuardTicks;
   private int lockSkewTicks;
   private boolean lockSkewYaw;
   private int lastAttackCount = -1;
   private int lastEntityId = Integer.MIN_VALUE;

   public SpookyTimeSmooth() {
      super("SpookyTime");
      this.resetNoise();
   }

   private void resetNoise() {
      this.noiseYaw = this.randGauss();
      this.noisePitch = this.randGauss();
      this.wanderYaw = this.randGauss();
      this.wanderPitch = this.randGauss();
   }

   private void resetState() {
      this.recoverTicks = 0;
      this.pursueStreak = 0;
      this.postHitTicks = 0;
      this.spikeGuardTicks = 0;
      this.lockSkewTicks = 0;
      this.lastStepYaw = this.lastStepPitch = 0.0F;
      this.lastRemYaw = Float.MAX_VALUE;
      this.lastRemPitch = 0.0F;
      this.lastCloseRatio = 0.0F;
      this.combatTicks = 0;
      this.rotationTicks = 0;
      this.speedDrift = 0.0F;
      this.lockSkewYaw = this.random.nextBoolean();
      this.resetNoise();
      this.model.resetPlayback();
   }

   private float strength() {
      return MathHelper.clamp(0.68F + this.speedDrift, 0.6F, 0.78F);
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
         this.lastEntityId = eid;
      }

      AttackHandler handler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
      int attackCount = handler.getCount();
      if (attackCount != this.lastAttackCount) {
         this.lastAttackCount = attackCount;
         this.postHitTicks = 1;
         this.combatTicks = 0;
         this.pursueStreak = 0;
         this.lastStepYaw = this.lastStepPitch = 0.0F;
         this.speedDrift = this.speedDrift + this.randGauss() * 0.015F;
         this.speedDrift = MathHelper.clamp(this.speedDrift, -0.08F, 0.08F);
      }

      if (this.combatTicks < 48) {
         this.combatTicks++;
      }

      this.rotationTicks++;
      KillAura aura = KillAura.getInstance();
      Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
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
      float hitRad = this.hitRadius(entity);
      boolean onTarget = entity != null && aura != null && RaytracingUtil.rayTrace(aura.attackDistance(), entity.getBoundingBox());
      boolean slowZone = onTarget || remaining < 13.0F;
      boolean fastApproach = !onTarget && remaining > 18.0F;
      boolean bridgeZone = !onTarget && remaining >= 13.0F && remaining <= 18.0F;
      boolean lockMode = onTarget || remaining < 13.0F;
      if (this.recoverTicks > 0) {
         this.recoverTicks--;
         return this.modelStep(currentAngle, remYaw, remPitch, hitRad, lockMode, true);
      }

      if (this.spikeGuardTicks > 0) {
         this.spikeGuardTicks--;
         return this.modelStep(currentAngle, remYaw, remPitch, hitRad, lockMode, true);
      }

      if (this.postHitTicks > 0 && slowZone) {
         this.postHitTicks--;
      }

      if (this.lastCloseRatio > 0.36F) {
         this.lockSkewTicks = 1;
         this.lockSkewYaw = !this.lockSkewYaw;
      }

      if (SpookyNewModel.isReady()) {
         float[] step = this.model.next(remYaw, remPitch, hitRad);
         if (step != null) {
            float overshoot = lockMode ? 1.04F : 1.08F;
            return this.commitModelStep(currentAngle, remYaw, remPitch, remaining, step[0], step[1], overshoot, onTarget, lockMode);
         }
      }

      return this.syntheticStep(currentAngle, remYaw, remPitch, remaining, velYaw, velPitch, onTarget, slowZone, fastApproach, bridgeZone, lockMode, hitRad);
   }

   private Angle modelStep(Angle current, float remYaw, float remPitch, float hitRad, boolean lockMode, boolean soft) {
      if (!SpookyNewModel.isReady()) {
         return this.finishStep(current, remYaw, remPitch, 0.0F, 0.0F, 1.01F);
      }

      float[] step = this.model.next(remYaw, remPitch, hitRad);
      if (step == null) {
         return this.finishStep(current, remYaw, remPitch, 0.0F, 0.0F, 1.01F);
      }

      if (soft) {
         step[0] *= this.lerp(0.42F, 0.62F);
         step[1] *= this.lerp(0.42F, 0.62F);
      }

      return this.commitModelStep(current, remYaw, remPitch, (float)Math.hypot(remYaw, remPitch), step[0], step[1], lockMode ? 1.03F : 1.06F, false, lockMode);
   }

   private Angle commitModelStep(
      Angle current, float remYaw, float remPitch, float remaining, float rawYaw, float rawPitch, float overshoot, boolean trackStreak, boolean lockMode
   ) {
      float stepYaw = this.quantizeDelta(rawYaw);
      float stepPitch = this.quantizeDelta(rawPitch);
      stepYaw = MathHelper.clamp(stepYaw, lockMode ? -34.0F : -40.0F, lockMode ? 34.0F : 40.0F);
      stepPitch = MathHelper.clamp(stepPitch, lockMode ? -24.0F : -28.0F, lockMode ? 24.0F : 28.0F);
      float errSq = remaining * remaining + 0.001F;
      float closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      if (lockMode && remaining < 18.0F && closeRatio > 0.22F) {
         float[] peeled = this.deflectStep(remYaw, remPitch, remaining, closeRatio, stepYaw, stepPitch);
         stepYaw = this.quantizeDelta(peeled[0]);
         stepPitch = this.quantizeDelta(peeled[1]);
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      }

      if (lockMode && remaining < 15.0F && closeRatio > 0.34F) {
         float[] peeled = this.deflectStep(remYaw, remPitch, remaining, closeRatio, stepYaw, stepPitch);
         stepYaw = this.quantizeDelta(peeled[0] * this.lerp(0.82F, 0.92F));
         stepPitch = this.quantizeDelta(peeled[1] * this.lerp(0.82F, 0.92F));
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      }

      if (lockMode && remaining < 14.0F && closeRatio > 0.4F) {
         stepYaw = this.quantizeDelta(stepYaw * this.lerp(0.55F, 0.72F));
         stepPitch = this.quantizeDelta(stepPitch * this.lerp(0.55F, 0.72F));
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
         if (closeRatio > 0.38F) {
            this.spikeGuardTicks = 1;
            this.lastCloseRatio = 0.0F;
            this.lastStepYaw = this.lastStepPitch = 0.0F;
            return this.finishStep(current, remYaw, remPitch, stepYaw * 0.35F, stepPitch * 0.35F, 1.02F);
         }
      }

      if (!lockMode && remaining < 16.0F && closeRatio > 0.4F) {
         this.spikeGuardTicks = 1;
         this.lastCloseRatio = 0.0F;
         this.lastStepYaw = this.lastStepPitch = 0.0F;
         return this.finishStep(current, remYaw, remPitch, this.quantizeDelta(stepYaw * 0.38F), this.quantizeDelta(stepPitch * 0.38F), 1.02F);
      }

      float stepMag = (float)Math.hypot(stepYaw, stepPitch);
      this.lastStepYaw = stepYaw;
      this.lastStepPitch = stepPitch;
      this.lastCloseRatio = closeRatio;
      if (trackStreak) {
         this.pursueStreak++;
         if (lockMode && closeRatio > 0.46F && remaining < 8.0F) {
            this.recoverTicks = 1;
         } else if (stepMag > 14.0F) {
            this.recoverTicks = 1;
         } else if (this.pursueStreak >= 4 + this.random.nextInt(2)) {
            this.recoverTicks = 1;
         }
      }

      float outMul = lockMode ? Math.min(overshoot, 1.05F) : overshoot;
      return this.finishStep(current, remYaw, remPitch, stepYaw, stepPitch, outMul);
   }

   private Angle syntheticStep(
      Angle current,
      float remYaw,
      float remPitch,
      float remaining,
      float velYaw,
      float velPitch,
      boolean onTarget,
      boolean slowZone,
      boolean fastApproach,
      boolean bridgeZone,
      boolean lockMode,
      float hitRad
   ) {
      this.advanceNoise();
      float strength = this.strength();
      float tremorY = this.noiseYaw * this.lerp(0.1F, 0.2F);
      float tremorP = this.noisePitch * this.lerp(0.08F, 0.16F);
      boolean combatWindow = this.combatTicks <= 40;
      float fatigue = this.rotationTicks > 200 ? 0.04F : (this.rotationTicks > 120 ? 0.02F : 0.0F);
      float maxFrac = (combatWindow && this.postHitTicks <= 0 ? 0.88F : 0.94F) - fatigue;
      if (fastApproach) {
         float frac = this.lerp(0.8F, 0.94F) * (0.68F + strength);
         float cap = this.lerp(44.0F, 62.0F) * (0.62F + strength);
         return this.cruise(current, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, frac, cap, 1.1F, false, false);
      }

      if (bridgeZone) {
         float frac = this.lerp(0.66F, 0.86F) * (0.64F + strength);
         float cap = this.lerp(30.0F, 44.0F) * (0.58F + strength);
         return this.cruise(current, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, frac, cap, 1.07F, false, true);
      }

      if (onTarget) {
         return this.lockOn(current, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, strength, maxFrac, hitRad);
      }

      float frac = remaining > 14.0F ? this.lerp(0.54F, 0.78F) * (0.62F + strength) : this.lerp(0.46F, 0.68F) * (0.58F + strength);
      frac = MathHelper.clamp(frac * (0.88F + this.random.nextFloat() * 0.16F), 0.36F, maxFrac);
      float cap = this.lerp(28.0F, 42.0F) * (0.54F + strength);
      return this.cruise(current, remYaw, remPitch, remaining, tremorY, tremorP, velYaw, velPitch, frac, cap, 1.06F, true, true);
   }

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

      float blend = this.lerp(0.08F, 0.14F);
      float rawYaw = this.lastStepYaw * blend + targetYaw * (1.0F - blend) + tremorY;
      float rawPitch = this.lastStepPitch * blend + targetPitch * (1.0F - blend) + tremorP;
      return this.commitSyntheticStep(current, remYaw, remPitch, remaining, rawYaw, rawPitch, tremorY, tremorP, overshoot, trackStreak, lockMode);
   }

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

      float yawFrac = MathHelper.clamp(this.lerp(0.48F, 0.68F) * (0.62F + strength), 0.38F, maxFrac);
      float pitchFrac = MathHelper.clamp(this.lerp(0.34F, 0.52F) * (0.58F + strength), 0.28F, maxFrac * 0.86F);
      if (this.lockSkewTicks > 0) {
         this.lockSkewTicks--;
         if (this.lockSkewYaw) {
            pitchFrac *= this.lerp(0.62F, 0.78F);
         } else {
            yawFrac *= this.lerp(0.62F, 0.78F);
         }
      }

      yawFrac *= 0.88F + this.random.nextFloat() * 0.14F;
      pitchFrac *= 0.88F + this.random.nextFloat() * 0.12F;
      float rawYaw = remYaw * yawFrac - velYaw * 0.05F + tremorY + this.wanderYaw * this.lerp(0.06F, 0.12F);
      float rawPitch = remPitch * pitchFrac - velPitch * 0.05F + tremorP + this.wanderPitch * this.lerp(0.05F, 0.1F);
      float blend = this.lerp(0.1F, 0.16F);
      rawYaw = this.lastStepYaw * blend + rawYaw * (1.0F - blend);
      rawPitch = this.lastStepPitch * blend + rawPitch * (1.0F - blend);
      float yawCap = this.lerp(24.0F, 36.0F) * (0.56F + strength);
      float pitchCap = this.lerp(16.0F, 26.0F) * (0.52F + strength);
      float yMag = Math.abs(rawYaw);
      float pMag = Math.abs(rawPitch);
      if (yMag > yawCap) {
         rawYaw *= yawCap / yMag;
      }

      if (pMag > pitchCap) {
         rawPitch *= pitchCap / pMag;
      }

      return this.commitSyntheticStep(current, remYaw, remPitch, remaining, rawYaw, rawPitch, tremorY, tremorP, 1.05F, true, true);
   }

   private Angle commitSyntheticStep(
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
      stepYaw = MathHelper.clamp(stepYaw, lockMode ? -34.0F : -40.0F, lockMode ? 34.0F : 40.0F);
      stepPitch = MathHelper.clamp(stepPitch, lockMode ? -24.0F : -28.0F, lockMode ? 24.0F : 28.0F);
      if (lockMode) {
         float yClose = Math.abs(stepYaw) / (Math.abs(remYaw) + 0.001F);
         float pClose = Math.abs(stepPitch) / (Math.abs(remPitch) + 0.001F);
         if (yClose > 0.76F) {
            stepYaw = this.quantizeDelta(stepYaw * this.lerp(0.72F, 0.84F));
         }

         if (pClose > 0.76F) {
            stepPitch = this.quantizeDelta(stepPitch * this.lerp(0.72F, 0.84F));
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
         stepYaw = this.quantizeDelta(peeled[0] * this.lerp(0.82F, 0.92F));
         stepPitch = this.quantizeDelta(peeled[1] * this.lerp(0.82F, 0.92F));
         stepMag = (float)Math.hypot(stepYaw, stepPitch);
         closeRatio = (stepYaw * remYaw + stepPitch * remPitch) / errSq;
      }

      if (!lockMode && remaining < 16.0F && closeRatio > 0.4F) {
         this.spikeGuardTicks = 1;
         this.lastCloseRatio = 0.0F;
         return this.finishStep(current, remYaw, remPitch, tremorY, tremorP, 1.01F);
      }

      this.lastStepYaw = stepYaw;
      this.lastStepPitch = stepPitch;
      this.lastCloseRatio = closeRatio;
      if (trackStreak) {
         this.pursueStreak++;
         if (lockMode && closeRatio > 0.46F && remaining < 8.0F) {
            this.recoverTicks = 1;
         } else if (stepMag > 14.0F) {
            this.recoverTicks = 1;
         } else if (this.pursueStreak >= 4 + this.random.nextInt(2)) {
            this.recoverTicks = 1;
         }
      }

      float outMul = lockMode ? Math.min(overshoot, 1.05F) : overshoot;
      return this.finishStep(current, remYaw, remPitch, stepYaw, stepPitch, outMul);
   }

   private float[] deflectStep(float remYaw, float remPitch, float remaining, float closeRatio, float stepYaw, float stepPitch) {
      float dirYaw = remYaw / (remaining + 0.001F);
      float dirPitch = remPitch / (remaining + 0.001F);
      float mag = (float)Math.hypot(stepYaw, stepPitch);
      float peel = this.lerp(0.1F, 0.22F) * MathHelper.clamp((closeRatio - 0.22F) / 0.14F, 0.0F, 1.0F);
      float outYaw = stepYaw - dirYaw * mag * peel - dirPitch * this.lerp(0.35F, 1.0F);
      float outPitch = stepPitch - dirPitch * mag * peel + dirYaw * this.lerp(0.25F, 0.9F) * 0.85F;
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
      this.speedDrift = this.speedDrift + (-0.06F * this.speedDrift + 0.03F * this.randGauss());
      this.speedDrift = MathHelper.clamp(this.speedDrift, -0.08F, 0.08F);
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
