package ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.AngleSmoothMode;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.util.aim.neuro.NeuroController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.string.chat.ChatMessage;

public class NeuroSmooth extends AngleSmoothMode {
   private final LinearSmoothMode fallback = new LinearSmoothMode();
   private long lastCallAt;
   private long lastFallbackWarnAt;

   public NeuroSmooth() {
      super("Neuro");
   }

   private void warnFallback(String reason) {
      long now = System.currentTimeMillis();
      if (now - this.lastFallbackWarnAt > 10000L) {
         this.lastFallbackWarnAt = now;
         ChatMessage.brandmessage("§cNeuro НЕ активна (§f" + reason + "§c) — ротация линейная, её палит ML-АЦ! Включи: §f.neuro play <имя>§c.");
      }
   }

   private double blocksTo(Entity entity) {
      return mc.player != null && entity != null ? mc.player.getEyePos().distanceTo(entity.getEntityPos().add(0.0, entity.getHeight() * 0.5, 0.0)) : 5.0;
   }

   private float hitRadius(Entity entity, double blocks) {
      if (entity == null) {
         return 1.5F;
      }

      double dist = Math.max(0.5, blocks);
      float halfW = (float)Math.toDegrees(Math.atan(entity.getWidth() * 0.5 / dist));
      float halfH = (float)Math.toDegrees(Math.atan(entity.getHeight() * 0.5 / dist));
      return MathHelper.clamp(Math.min(halfW, halfH) * 0.8F, 0.8F, 14.0F);
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      NeuroController controller = NeuroController.get();
      if (!controller.isPlaying()) {
         this.warnFallback("модель не запущена");
         return this.fallback.limitAngleChange(currentAngle, targetAngle, vec3d, entity);
      }

      long now = System.currentTimeMillis();
      if (now - this.lastCallAt > 400L) {
         controller.resetPlayback();
      }

      this.lastCallAt = now;
      Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      double blocks = this.blocksTo(entity);
      float hit = this.hitRadius(entity, blocks);
      float[] step = controller.nextStep(delta.getYaw(), delta.getPitch(), hit);
      if (step == null) {
         this.warnFallback("модель пуста");
         return this.fallback.limitAngleChange(currentAngle, targetAngle, vec3d, entity);
      } else {
         return apply(currentAngle, step[0], step[1]);
      }
   }

   @Override
   public Vec3d randomValue() {
      return new Vec3d(0.0, 0.0, 0.0);
   }

   private static Angle apply(Angle current, float stepYaw, float stepPitch) {
      return new Angle(current.getYaw() + stepYaw, MathHelper.clamp(current.getPitch() + stepPitch, -90.0F, 90.0F));
   }
}
