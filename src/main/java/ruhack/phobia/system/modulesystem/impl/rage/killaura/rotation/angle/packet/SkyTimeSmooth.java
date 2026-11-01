package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

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

/**
 * Rotation for SkyTime (Shard / SlothAC AI combat).
 * Traits the model watches for: zero-lag lead, constant angular velocity,
 * perfect axis coupling, post-hit snap. This mode breaks all four.
 */
public class SkyTimeSmooth extends AngleSmoothMode {
   private final Random random = new Random(0x5A71F1MEL ^ System.nanoTime());

   private float yawVel;
   private float pitchVel;
   private float noiseYaw;
   private float noisePitch;
   private float wanderYaw;
   private float wanderPitch;
   private float speedDrift;
   private int reactionTicks;
   private int postHitTicks;
   private int combatTicks;
   private int lastAttackCount = -1;
   private int lastEntityId = Integer.MIN_VALUE;
   private float lastStepYaw;
   private float lastStepPitch;

   private static final float YAW_GAIN = 0.40F;
   private static final float PITCH_GAIN = 0.26F;
   private static final float DAMPING = 0.76F;
   private static final float NOISE_THETA = 0.18F;
   private static final float NOISE_SIGMA = 0.52F;
   private static final float WANDER_THETA = 0.05F;
   private static final float WANDER_SIGMA = 0.11F;

   public SkyTimeSmooth() {
      super("SkyTime");
      this.resetState();
   }

   private void resetState() {
      this.yawVel = 0.0F;
      this.pitchVel = 0.0F;
      this.noiseYaw = this.randGauss();
      this.noisePitch = this.randGauss();
      this.wanderYaw = this.randGauss();
      this.wanderPitch = this.randGauss();
      this.speedDrift = 0.0F;
      this.reactionTicks = 2 + this.random.nextInt(4);
      this.postHitTicks = 0;
      this.combatTicks = 0;
      this.lastStepYaw = 0.0F;
      this.lastStepPitch = 0.0F;
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      int eid = entity == null ? Integer.MIN_VALUE : entity.getId();
      if (eid != this.lastEntityId) {
         this.resetState();
         this.lastEntityId = eid;
      }

      AttackHandler handler = null;
      try {
         handler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
      } catch (Throwable ignored) {
      }

      if (handler != null) {
         int attackCount = handler.getCount();
         if (attackCount != this.lastAttackCount) {
            this.lastAttackCount = attackCount;
            this.postHitTicks = 3 + this.random.nextInt(3);
            this.reactionTicks = 1 + this.random.nextInt(3);
            this.yawVel *= 0.42F;
            this.pitchVel *= 0.38F;
            this.speedDrift += this.randGauss() * 0.02F;
         }
      }

      if (this.combatTicks < 64) {
         this.combatTicks++;
      }

      Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float remYaw = delta.getYaw();
      float remPitch = delta.getPitch();

      // delayed reaction on target switches / velocity (AI flags 0-lag lead)
      if (this.reactionTicks > 0) {
         this.reactionTicks--;
         remYaw *= 0.35F + this.random.nextFloat() * 0.25F;
         remPitch *= 0.30F + this.random.nextFloat() * 0.20F;
      }

      // independent axis gains (AI flags coupled axes)
      float yawAcc = remYaw * YAW_GAIN * (0.82F + this.random.nextFloat() * 0.28F);
      float pitchAcc = remPitch * PITCH_GAIN * (0.78F + this.random.nextFloat() * 0.24F);

      if (this.postHitTicks > 0) {
         this.postHitTicks--;
         yawAcc *= 0.40F;
         pitchAcc *= 0.35F;
      }

      this.yawVel = this.yawVel * DAMPING + yawAcc;
      this.pitchVel = this.pitchVel * DAMPING + pitchAcc;

      float maxYaw = 24.0F + this.random.nextFloat() * 10.0F + this.speedDrift * 20.0F;
      float maxPitch = 14.0F + this.random.nextFloat() * 6.0F;
      this.yawVel = MathHelper.clamp(this.yawVel, -maxYaw, maxYaw);
      this.pitchVel = MathHelper.clamp(this.pitchVel, -maxPitch, maxPitch);

      // Ornstein–Uhlenbeck tremor + slow wander
      this.noiseYaw += -NOISE_THETA * this.noiseYaw + NOISE_SIGMA * this.randGauss();
      this.noisePitch += -NOISE_THETA * this.noisePitch + NOISE_SIGMA * 0.65F * this.randGauss();
      this.wanderYaw += -WANDER_THETA * this.wanderYaw + WANDER_SIGMA * this.randGauss();
      this.wanderPitch += -WANDER_THETA * this.wanderPitch + WANDER_SIGMA * this.randGauss();
      this.speedDrift += -0.05F * this.speedDrift + 0.025F * this.randGauss();
      this.speedDrift = MathHelper.clamp(this.speedDrift, -0.1F, 0.1F);

      float stepYaw = this.yawVel + this.noiseYaw * 0.32F + this.wanderYaw * 0.55F;
      float stepPitch = this.pitchVel + this.noisePitch * 0.24F + this.wanderPitch * 0.40F;

      // break constant angular velocity (step correlation)
      if (Math.abs(stepYaw - this.lastStepYaw) < 0.08F && Math.abs(stepYaw) > 0.4F) {
         stepYaw += (this.random.nextFloat() - 0.5F) * 0.55F;
      }
      if (Math.abs(stepPitch - this.lastStepPitch) < 0.06F && Math.abs(stepPitch) > 0.25F) {
         stepPitch += (this.random.nextFloat() - 0.5F) * 0.35F;
      }

      stepYaw = this.quantizeDelta(stepYaw);
      stepPitch = this.quantizeDelta(stepPitch);

      this.lastStepYaw = stepYaw;
      this.lastStepPitch = stepPitch;

      float overshoot = 1.05F + this.random.nextFloat() * 0.18F;
      float outYaw = currentAngle.getYaw() + clampOvershoot(stepYaw, remYaw, overshoot);
      float outPitch = MathHelper.clamp(currentAngle.getPitch() + clampOvershoot(stepPitch, remPitch, overshoot), -89.5F, 89.5F);
      return new Angle(outYaw, outPitch);
   }

   private float quantizeDelta(float delta) {
      double gcd = MathUtils.computeGcd();
      if (gcd <= 1.0E-6) {
         return delta;
      }
      long steps = Math.round((double)delta / gcd);
      if (steps == 0L && (double)Math.abs(delta) > gcd * 0.3) {
         steps = delta > 0.0F ? 1L : -1L;
      }
      return (float)((double)steps * gcd);
   }

   private static float clampOvershoot(float step, float remaining, float mul) {
      float r = Math.abs(remaining);
      return MathHelper.clamp(step, -r * mul, r * mul);
   }

   private float randGauss() {
      return (float)this.random.nextGaussian();
   }

   @Override
   public Vec3d randomValue() {
      double s = 0.0021;
      return new Vec3d((this.random.nextDouble() * 2.0 - 1.0) * s, (this.random.nextDouble() * 2.0 - 1.0) * s, (this.random.nextDouble() * 2.0 - 1.0) * s);
   }
}
