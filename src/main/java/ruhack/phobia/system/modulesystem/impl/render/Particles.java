package ruhack.phobia.system.modulesystem.impl.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos.Mutable;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.api.EventManager;
import ruhack.phobia.system.eventsystem.impl.AttackEvent;
import ruhack.phobia.system.eventsystem.impl.PacketEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.other.Theme;
import ruhack.phobia.system.modulesystem.setting.implement.ColorSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.rendersystem.render3d.glow.Glow3D;
import ruhack.phobia.util.colorschem.ColorUtil;
import ruhack.phobia.util.render.GradientUtil;

public final class Particles extends ModuleStructure {
   private static final float TAU = (float) (Math.PI * 2);
   private static final float MAX_DELTA = 0.05F;
   private static final float RISE_LIFETIME = 6.5F;
   private static final float FALL_LIFETIME = 8.0F;
   private static final float PEARL_LIFETIME = 2.25F;
   private static final float CRITICAL_LIFETIME = 1.15F;
   private static final float TOTEM_LIFETIME = 1.45F;
   private static final int MAX_EVENT_PARTICLES = 420;
   private static final int TOTEM_PARTICLE_COUNT = 45;
   private final List<Particles.GlowParticle> particles = new ArrayList<>(96);
   private final ArrayDeque<Particles.GlowParticle> particlePool = new ArrayDeque<>(150);
   private final Mutable collisionPos = new Mutable();
   private final Mutable spawnPos = new Mutable();
   private final MultiSelectSetting reasons = new MultiSelectSetting("Добавлять при", "Условия появления частиц")
      .value("Бездействие", "Бросок перла", "Крит удар", "Снос тотема")
      .selected("Бездействие");
   private final SelectSetting mode = new SelectSetting("Режим", "Физика движения частиц", "Взлёт", "Взлёт", "Падение");
   private final SliderSettings count = new SliderSettings("Количество", "Количество частиц", 55.0F).range(5, 150);
   private final SliderSettings size = new SliderSettings("Размер", "Размер Glow", 0.14F).range(0.04F, 0.35F).step(0.01F);
   private final SliderSettings radius = new SliderSettings("Радиус", "Радиус появления вокруг игрока", 9.0F).range(3, 24);
   private final SliderSettings speed = new SliderSettings("Скорость", "Скорость взлёта или падения", 1.0F).range(0.3F, 2.5F).step(0.05F);
   private final SelectSetting colorMode = new SelectSetting("Цвет", "Источник цвета Glow", "От темы", "От темы", "Свой");
   private final ColorSetting color = new ColorSetting("Свой цвет", "Цвет частиц")
      .value(ColorUtil.getColor(150, 60, 255, 255))
      .visible(() -> this.colorMode.isSelected("Свой"));
   private long lastFrameNanos;
   private float spawnAccumulator;
   private boolean lastFallingMode;
   private int ambientParticleCount;

   public Particles() {
      super("Particles", "Создаёт glow-частицы в мире, за жемчугом и при критических ударах", ModuleCategory.RENDER);
      this.settings(this.reasons, this.mode, this.count, this.size, this.radius, this.speed, this.colorMode, this.color);
   }

   @Override
   public void activate() {
      this.recycleParticles();
      this.spawnAccumulator = 0.0F;
      this.lastFrameNanos = System.nanoTime();
      this.lastFallingMode = this.isFallingMode();
      EventManager.register(this);
   }

   @Override
   public void deactivate() {
      EventManager.unregister(this);
      this.recycleParticles();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         boolean fallingMode = this.isFallingMode();
         if (fallingMode != this.lastFallingMode) {
            this.recycleParticles();
            this.spawnAccumulator = 0.0F;
            this.lastFallingMode = fallingMode;
         }

         long now = System.nanoTime();
         float delta = Math.min(0.05F, Math.max(0.0F, (float)(now - this.lastFrameNanos) * 1.0E-9F));
         this.lastFrameNanos = now;
         this.update(delta);
         if (this.reasons.isSelected("Бездействие")) {
            this.spawn(delta, fallingMode);
         } else {
            this.spawnAccumulator = 0.0F;
         }

         this.render();
      } else {
         this.recycleParticles();
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.reasons.isSelected("Бросок перла") && mc.player != null && mc.world != null) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity pearl && !pearl.isOnGround() && pearl.getVelocity().lengthSquared() > 1.0E-4) {
               this.spawnPearlTrail(pearl);
            }
         }
      }
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (this.reasons.isSelected("Крит удар") && event.getTarget() instanceof LivingEntity target && this.isCriticalHit()) {
         this.spawnCriticalBurst(target);
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.getType() == PacketEvent.Type.RECEIVE) {
         if (this.reasons.isSelected("Снос тотема")
            && event.getPacket() instanceof EntityStatusS2CPacket status
            && status.getStatus() == 35
            && mc.world != null
            && status.getEntity(mc.world) instanceof LivingEntity living) {
            this.spawnTotemBurst(living);
         }
      }
   }

   private void update(float delta) {
      float speedScale = this.speed.getValue();
      float time = (float)(System.nanoTime() & Long.MAX_VALUE) * 1.0E-9F;
      double pearlDrag = Math.pow(0.965, delta * 60.0);

      for (int i = this.particles.size() - 1; i >= 0; i--) {
         Particles.GlowParticle particle = this.particles.get(i);
         particle.age += delta;
         if (particle.age >= particle.maxAge) {
            this.removeParticle(i, particle);
         } else if (particle.kind != Particles.ParticleKind.AMBIENT) {
            if (!this.updateEventParticle(particle, delta, pearlDrag)) {
               this.removeParticle(i, particle);
            }
         } else {
            double flowX = Math.sin(particle.z * 0.31 + time * 0.72 + particle.phase) * 0.16 + Math.cos(particle.y * 0.43 - time * 0.37) * 0.07;
            double flowZ = Math.cos(particle.x * 0.29 - time * 0.66 + particle.phase) * 0.16 - Math.sin(particle.y * 0.39 + time * 0.41) * 0.07;
            double ax = flowX - particle.vx * 0.62;
            double az = flowZ - particle.vz * 0.62;
            double ay;
            if (particle.falling) {
               ay = -1.75 * speedScale - particle.vy * Math.abs(particle.vy) * 0.34;
            } else {
               ay = 0.82 * speedScale - particle.vy * 0.58;
            }

            particle.vx += ax * delta;
            particle.vy += ay * delta;
            particle.vz += az * delta;
            double maxVertical = (particle.falling ? 2.8 : 1.7) * speedScale;
            particle.vy = Math.max(-maxVertical, Math.min(maxVertical, particle.vy));
            double nextX = particle.x + particle.vx * delta;
            double nextY = particle.y + particle.vy * delta;
            double nextZ = particle.z + particle.vz * delta;
            if (this.isFree(nextX, nextY, nextZ, this.collisionPos) && (!particle.falling || !this.isGroundClose(nextX, nextY, nextZ, this.collisionPos))) {
               particle.x = nextX;
               particle.y = nextY;
               particle.z = nextZ;
            } else {
               this.removeParticle(i, particle);
            }
         }
      }
   }

   private void spawn(float delta, boolean falling) {
      int targetCount = this.count.getInt();
      int ambientCount = this.ambientParticleCount;
      if (ambientCount < targetCount) {
         float maxAge = falling ? 8.0F : 6.5F;
         this.spawnAccumulator += delta * targetCount / maxAge;
         int amount = Math.min(targetCount - ambientCount, (int)this.spawnAccumulator);
         if (ambientCount == 0) {
            amount = Math.min(targetCount, Math.max(amount, targetCount / 4));
         }

         this.spawnAccumulator = this.spawnAccumulator - Math.min(this.spawnAccumulator, amount);
         ThreadLocalRandom random = ThreadLocalRandom.current();
         double centerX = mc.player.getX();
         double centerY = mc.player.getY();
         double centerZ = mc.player.getZ();
         float spawnRadius = this.radius.getValue();
         Mutable pos = this.spawnPos;

         for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * (float) (Math.PI * 2);
            double distance = Math.sqrt(random.nextDouble()) * spawnRadius;
            double x = centerX + Math.cos(angle) * distance;
            double z = centerZ + Math.sin(angle) * distance;
            double y;
            if (falling) {
               y = centerY + 4.5 + random.nextDouble() * 6.0;
            } else {
               double ground = this.findGround(x, z, centerY + 3.0, pos);
               if (Double.isNaN(ground)) {
                  continue;
               }

               y = ground + 0.08 + random.nextDouble() * 0.12;
            }

            if (this.isFree(x, y, z, pos)) {
               float particleSize = this.size.getValue() * (0.75F + random.nextFloat() * 0.5F);
               float life = maxAge * (0.82F + random.nextFloat() * 0.36F);
               double vx = (random.nextDouble() - 0.5) * 0.22;
               double vz = (random.nextDouble() - 0.5) * 0.22;
               double vy = falling ? -(0.08 + random.nextDouble() * 0.18) : 0.18 + random.nextDouble() * 0.28;
               float phase = random.nextFloat() * (float) (Math.PI * 2);
               Particles.GlowParticle particle = this.particlePool.pollFirst();
               if (particle == null) {
                  particle = new Particles.GlowParticle();
               }

               particle.reset(x, y, z, vx, vy, vz, particleSize, phase, life, falling, Particles.ParticleKind.AMBIENT);
               this.particles.add(particle);
               this.ambientParticleCount++;
            }
         }
      }
   }

   private void spawnPearlTrail(EnderPearlEntity pearl) {
      if (this.particles.size() < 420) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         float baseSize = this.size.getValue();

         for (int i = 0; i < 2 && this.particles.size() < 420; i++) {
            double angle = random.nextDouble() * (float) (Math.PI * 2);
            double velocity = random.nextDouble(0.9, 2.25);
            this.addEventParticle(
               pearl.getX(),
               pearl.getY() + random.nextDouble(0.1, 0.35),
               pearl.getZ(),
               Math.cos(angle) * velocity,
               random.nextDouble(-0.36, 0.36),
               Math.sin(angle) * velocity,
               baseSize * (0.75F + random.nextFloat() * 0.45F),
               2.25F * (0.88F + random.nextFloat() * 0.24F),
               Particles.ParticleKind.PEARL
            );
         }
      }
   }

   private void spawnCriticalBurst(LivingEntity target) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int available = 420 - this.particles.size();
      int amount = Math.min(35, Math.max(0, available));
      float baseSize = this.size.getValue();

      for (int i = 0; i < amount; i++) {
         this.addEventParticle(
            target.getX() + random.nextDouble(-0.4, 0.4),
            target.getY() + random.nextDouble(-0.5, target.getHeight() + 0.4),
            target.getZ() + random.nextDouble(-0.4, 0.4),
            random.nextDouble(-1.6, 1.6) * 4.2,
            random.nextDouble(-0.25, 1.4) * 4.2,
            random.nextDouble(-1.6, 1.6) * 4.2,
            baseSize * (0.8F + random.nextFloat() * 0.55F),
            1.15F * (0.87F + random.nextFloat() * 0.26F),
            Particles.ParticleKind.CRITICAL,
            0
         );
      }
   }

   private void spawnTotemBurst(LivingEntity target) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int amount = Math.min(45, Math.max(0, 420 - this.particles.size()));
      double centerY = target.getY() + target.getHeight() * 0.52;
      float baseSize = this.size.getValue();

      for (int i = 0; i < amount; i++) {
         double angle = random.nextDouble() * (float) (Math.PI * 2);
         double horizontalSpeed = random.nextDouble(0.9, 2.7);
         int particleColor;
         if (random.nextBoolean()) {
            particleColor = rgb(153 + random.nextInt(52), 153 + random.nextInt(77), random.nextInt(52));
         } else {
            particleColor = rgb(26 + random.nextInt(52), 102 + random.nextInt(77), random.nextInt(52));
         }

         this.addEventParticle(
            target.getX() + random.nextDouble(-0.32, 0.32),
            centerY + random.nextDouble(-0.45, 0.7),
            target.getZ() + random.nextDouble(-0.32, 0.32),
            Math.cos(angle) * horizontalSpeed,
            random.nextDouble(0.3, 2.1),
            Math.sin(angle) * horizontalSpeed,
            baseSize * (0.82F + random.nextFloat() * 0.58F),
            1.45F * (0.82F + random.nextFloat() * 0.36F),
            Particles.ParticleKind.TOTEM,
            particleColor
         );
      }
   }

   private void addEventParticle(double x, double y, double z, double vx, double vy, double vz, float particleSize, float lifetime, Particles.ParticleKind kind) {
      this.addEventParticle(x, y, z, vx, vy, vz, particleSize, lifetime, kind, 0);
   }

   private void addEventParticle(
      double x, double y, double z, double vx, double vy, double vz, float particleSize, float lifetime, Particles.ParticleKind kind, int particleColor
   ) {
      if (this.isFree(x, y, z, this.spawnPos)) {
         Particles.GlowParticle particle = this.particlePool.pollFirst();
         if (particle == null) {
            particle = new Particles.GlowParticle();
         }

         particle.reset(
            x, y, z, vx, vy, vz, particleSize, ThreadLocalRandom.current().nextFloat() * (float) (Math.PI * 2), lifetime, false, kind, particleColor
         );
         this.particles.add(particle);
      }
   }

   private boolean updateEventParticle(Particles.GlowParticle particle, float delta, double pearlDrag) {
      if (particle.kind != Particles.ParticleKind.PEARL) {
         return this.updateBurstParticle(particle, delta);
      }

      particle.vx *= pearlDrag;
      particle.vz *= pearlDrag;
      particle.vy = particle.vy * pearlDrag - 0.12 * delta;
      double nextX = particle.x + particle.vx * delta;
      double nextY = particle.y + particle.vy * delta;
      double nextZ = particle.z + particle.vz * delta;
      if (!this.isFree(nextX, nextY, nextZ, this.collisionPos)) {
         return false;
      }

      particle.x = nextX;
      particle.y = nextY;
      particle.z = nextZ;
      return true;
   }

   private boolean updateBurstParticle(Particles.GlowParticle particle, float delta) {
      float progress = Math.min(1.0F, particle.age / Math.max(0.001F, particle.maxAge));
      double smooth = particle.kind == Particles.ParticleKind.CRITICAL ? 0.5 : 0.6;
      double motion = Math.pow(1.0 - progress, smooth) * delta;
      double bounce = 0.8;
      double nextX = particle.x + particle.vx * motion;
      if (!this.isFree(nextX, particle.y, particle.z, this.collisionPos)) {
         particle.vx *= -bounce;
         nextX = particle.x;
      }

      double nextY = particle.y + particle.vy * motion;
      if (!this.isFree(nextX, nextY, particle.z, this.collisionPos)) {
         particle.vy *= -bounce;
         nextY = particle.y;
      }

      double nextZ = particle.z + particle.vz * motion;
      if (!this.isFree(nextX, nextY, nextZ, this.collisionPos)) {
         particle.vz *= -bounce;
         nextZ = particle.z;
      }

      double velocityRetention = Math.pow(0.9999, delta * 60.0);
      particle.vx *= velocityRetention;
      particle.vy = particle.vy * velocityRetention - (particle.kind == Particles.ParticleKind.CRITICAL ? 1.8 : 0.72) * delta;
      particle.vz *= velocityRetention;
      particle.x = nextX;
      particle.y = nextY;
      particle.z = nextZ;
      return true;
   }

   private boolean isCriticalHit() {
      return mc.player != null
         && mc.player.getAttackCooldownProgress(0.5F) > 0.9F
         && mc.player.fallDistance > 0.0
         && !mc.player.isOnGround()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !mc.player.isSprinting()
         && !mc.player.hasVehicle();
   }

   private void render() {
      if (!this.particles.isEmpty()) {
         boolean themeColor = this.colorMode.isSelected("От темы");
         int customColor = themeColor ? 0 : this.color.getColorNoAlpha();
         Theme.Palette themePalette = null;
         if (themeColor) {
            Theme theme = Theme.getInstance();
            if (theme != null) {
               theme.getColorAt(0.0F, 255);
               themePalette = theme.selectedPalette();
            }
         }

         Glow3D.begin(false);

         for (Particles.GlowParticle particle : this.particles) {
            float life = particle.age / particle.maxAge;
            float alpha = particle.kind == Particles.ParticleKind.AMBIENT && particle.falling
               ? smoothStep(0.0F, 0.1F, life)
               : smoothStep(0.0F, particle.kind == Particles.ParticleKind.AMBIENT ? 0.14F : 0.06F, life)
                  * (1.0F - smoothStep(particle.kind == Particles.ParticleKind.AMBIENT ? 0.72F : 0.62F, 1.0F, life));
            float pulse = 0.94F + 0.06F * (float)Math.sin(particle.phase + particle.age * 2.0F);
            int particleColor = this.particleColor(particle, life, themeColor, customColor, themePalette);
            Glow3D.glow(particle.x, particle.y, particle.z, particle.size * pulse * 3.1F, particleColor, alpha * 0.82F);
         }

         Glow3D.end();
      }
   }

   private int particleColor(Particles.GlowParticle particle, float life, boolean themeColor, int customColor, Theme.Palette themePalette) {
      if (particle.color != 0) {
         return particle.color;
      }

      if (!themeColor) {
         return customColor;
      }

      float position = (particle.phase / (float) (Math.PI * 2) + life * 0.12F) % 1.0F;
      return themePalette != null ? GradientUtil.color(18, Math.round(position * 360.0F), themePalette.colors()) : ColorUtil.getClientColorAt(position);
   }

   private static int rgb(int red, int green, int blue) {
      return 0xFF000000 | red << 16 | green << 8 | blue;
   }

   private double findGround(double x, double z, double startY, Mutable pos) {
      int start = (int)Math.floor(startY);
      int end = Math.max(mc.world.getBottomY(), start - 18);

      for (int y = start; y >= end; y--) {
         pos.set(x, y, z);
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
            return y + 1.0;
         }
      }

      return Double.NaN;
   }

   private boolean isGroundClose(double x, double y, double z, Mutable pos) {
      return !this.isFree(x, y - 0.13, z, pos);
   }

   private boolean isFree(double x, double y, double z, Mutable pos) {
      pos.set(x, y, z);
      BlockState state = mc.world.getBlockState(pos);
      return state.isAir() || state.getCollisionShape(mc.world, pos).isEmpty();
   }

   private boolean isFallingMode() {
      return this.mode.isSelected("Падение");
   }

   private void recycleParticles() {
      for (Particles.GlowParticle particle : this.particles) {
         this.recycle(particle);
      }

      this.particles.clear();
      this.ambientParticleCount = 0;
   }

   private void removeParticle(int index, Particles.GlowParticle particle) {
      int lastIndex = this.particles.size() - 1;
      if (index != lastIndex) {
         this.particles.set(index, this.particles.get(lastIndex));
      }

      this.particles.removeLast();
      if (particle.kind == Particles.ParticleKind.AMBIENT) {
         this.ambientParticleCount--;
      }

      this.recycle(particle);
   }

   private void recycle(Particles.GlowParticle particle) {
      if (this.particlePool.size() < Math.max(this.count.getInt(), 150)) {
         this.particlePool.addFirst(particle);
      }
   }

   private static float smoothStep(float edge0, float edge1, float value) {
      float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
      return t * t * (3.0F - 2.0F * t);
   }

   private static final class GlowParticle {
      double x;
      double y;
      double z;
      double vx;
      double vy;
      double vz;
      float size;
      float phase;
      float maxAge;
      boolean falling;
      Particles.ParticleKind kind;
      int color;
      float age;

      void reset(
         double x, double y, double z, double vx, double vy, double vz, float size, float phase, float maxAge, boolean falling, Particles.ParticleKind kind
      ) {
         this.reset(x, y, z, vx, vy, vz, size, phase, maxAge, falling, kind, 0);
      }

      void reset(
         double x,
         double y,
         double z,
         double vx,
         double vy,
         double vz,
         float size,
         float phase,
         float maxAge,
         boolean falling,
         Particles.ParticleKind kind,
         int color
      ) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.size = size;
         this.phase = phase;
         this.maxAge = maxAge;
         this.falling = falling;
         this.kind = kind;
         this.color = color;
         this.age = 0.0F;
      }
   }

   private enum ParticleKind {
      AMBIENT,
      PEARL,
      CRITICAL,
      TOTEM;
   }
}
