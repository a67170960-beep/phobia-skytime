package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.Initialization;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackHandler;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.util.math.MathUtils;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;

public class SkyTimeSmooth extends AngleSmoothMode {
   private final Random random = new Random(0x5A71F1L ^ System.nanoTime());
   private float yawVel, pitchVel, noiseYaw, noisePitch, wanderYaw, wanderPitch, speedDrift;
   private int reactionTicks, postHitTicks, combatTicks, lastAttackCount = -1, lastEntityId = Integer.MIN_VALUE;
   private float lastStepYaw, lastStepPitch;
   private static final float YAW_GAIN = 0.40F, PITCH_GAIN = 0.26F, DAMPING = 0.76F;
   private static final float NOISE_THETA = 0.18F, NOISE_SIGMA = 0.52F, WANDER_THETA = 0.05F, WANDER_SIGMA = 0.11F;

   public SkyTimeSmooth() { super("SkyTime"); this.resetState(); }

   private void resetState() {
      yawVel = pitchVel = 0; noiseYaw = noisePitch = wanderYaw = wanderPitch = randGauss();
      speedDrift = 0; reactionTicks = 2 + random.nextInt(4); postHitTicks = combatTicks = 0;
      lastStepYaw = lastStepPitch = 0;
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      int eid = entity == null ? Integer.MIN_VALUE : entity.getId();
      if (eid != lastEntityId) { resetState(); lastEntityId = eid; }
      AttackHandler handler = null;
      try { handler = Initialization.getInstance().getManager().getAttackPerpetrator().getAttackHandler(); } catch (Throwable ignored) {}
      if (handler != null) {
         int ac = handler.getCount();
         if (ac != lastAttackCount) {
            lastAttackCount = ac; postHitTicks = 3 + random.nextInt(3); reactionTicks = 1 + random.nextInt(3);
            yawVel *= 0.42F; pitchVel *= 0.38F; speedDrift += randGauss() * 0.02F;
         }
      }
      if (combatTicks < 64) combatTicks++;
      Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float remYaw = delta.getYaw(), remPitch = delta.getPitch();
      if (reactionTicks > 0) { reactionTicks--; remYaw *= 0.35F + random.nextFloat() * 0.25F; remPitch *= 0.30F + random.nextFloat() * 0.20F; }
      float yawAcc = remYaw * YAW_GAIN * (0.82F + random.nextFloat() * 0.28F);
      float pitchAcc = remPitch * PITCH_GAIN * (0.78F + random.nextFloat() * 0.24F);
      if (postHitTicks > 0) { postHitTicks--; yawAcc *= 0.40F; pitchAcc *= 0.35F; }
      yawVel = yawVel * DAMPING + yawAcc; pitchVel = pitchVel * DAMPING + pitchAcc;
      float maxYaw = 24.0F + random.nextFloat() * 10.0F + speedDrift * 20.0F;
      float maxPitch = 14.0F + random.nextFloat() * 6.0F;
      yawVel = MathHelper.clamp(yawVel, -maxYaw, maxYaw);
      pitchVel = MathHelper.clamp(pitchVel, -maxPitch, maxPitch);
      noiseYaw += -NOISE_THETA * noiseYaw + NOISE_SIGMA * randGauss();
      noisePitch += -NOISE_THETA * noisePitch + NOISE_SIGMA * 0.65F * randGauss();
      wanderYaw += -WANDER_THETA * wanderYaw + WANDER_SIGMA * randGauss();
      wanderPitch += -WANDER_THETA * wanderPitch + WANDER_SIGMA * randGauss();
      speedDrift = MathHelper.clamp(speedDrift + (-0.05F * speedDrift + 0.025F * randGauss()), -0.1F, 0.1F);
      float stepYaw = yawVel + noiseYaw * 0.32F + wanderYaw * 0.55F;
      float stepPitch = pitchVel + noisePitch * 0.24F + wanderPitch * 0.40F;
      if (Math.abs(stepYaw - lastStepYaw) < 0.08F && Math.abs(stepYaw) > 0.4F) stepYaw += (random.nextFloat() - 0.5F) * 0.55F;
      if (Math.abs(stepPitch - lastStepPitch) < 0.06F && Math.abs(stepPitch) > 0.25F) stepPitch += (random.nextFloat() - 0.5F) * 0.35F;
      stepYaw = quantizeDelta(stepYaw); stepPitch = quantizeDelta(stepPitch);
      lastStepYaw = stepYaw; lastStepPitch = stepPitch;
      float overshoot = 1.05F + random.nextFloat() * 0.18F;
      return new Angle(currentAngle.getYaw() + clampOvershoot(stepYaw, remYaw, overshoot),
         MathHelper.clamp(currentAngle.getPitch() + clampOvershoot(stepPitch, remPitch, overshoot), -89.5F, 89.5F));
   }

   private float quantizeDelta(float delta) {
      double gcd = MathUtils.computeGcd();
      if (gcd <= 1.0E-6) return delta;
      long steps = Math.round((double) delta / gcd);
      if (steps == 0L && (double) Math.abs(delta) > gcd * 0.3) steps = delta > 0 ? 1L : -1L;
      return (float) ((double) steps * gcd);
   }

   private static float clampOvershoot(float step, float remaining, float mul) {
      float r = Math.abs(remaining);
      return MathHelper.clamp(step, -r * mul, r * mul);
   }

   private float randGauss() { return (float) random.nextGaussian(); }

   @Override
   public Vec3d randomValue() {
      double s = 0.0021;
      return new Vec3d((random.nextDouble() * 2 - 1) * s, (random.nextDouble() * 2 - 1) * s, (random.nextDouble() * 2 - 1) * s);
   }
}
