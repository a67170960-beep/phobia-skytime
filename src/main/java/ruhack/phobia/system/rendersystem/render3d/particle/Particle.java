package ruhack.phobia.system.rendersystem.render3d.particle;

import java.util.Random;

public class Particle {
   private static final Random RANDOM = new Random();
   public double x;
   public double y;
   public double z;
   public double vx;
   public double vy;
   public double vz;
   public float size;
   public float rotationY;
   public float rotationX;
   public float rotationSpeedY;
   public float rotationSpeedX;
   public int lifetime;
   public int maxLifetime;
   public float phase;
   public float initialSize;
   public float spiralAngle;
   public float spiralRadius;

   public Particle(double x, double y, double z, float size, int lifetime) {
      this.reset(x, y, z, size, lifetime);
   }

   public void reset(double x, double y, double z, float size, int lifetime) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.size = size;
      this.initialSize = size;
      this.maxLifetime = lifetime;
      this.lifetime = lifetime;
      this.vx = 0.0;
      this.vy = 0.0;
      this.vz = 0.0;
      this.rotationY = RANDOM.nextFloat() * 360.0F;
      this.rotationX = RANDOM.nextFloat() * 360.0F;
      this.rotationSpeedY = 0.0F;
      this.rotationSpeedX = 0.0F;
      this.phase = RANDOM.nextFloat() * (float) Math.PI * 2.0F;
      this.spiralAngle = RANDOM.nextFloat() * (float) Math.PI * 2.0F;
      this.spiralRadius = 0.0F;
   }

   public void setVelocity(double vx, double vy, double vz) {
      this.vx = vx;
      this.vy = vy;
      this.vz = vz;
   }

   public void setRotationSpeed(float speedY, float speedX) {
      this.rotationSpeedY = speedY;
      this.rotationSpeedX = speedX;
   }

   public void update(
      float gravity,
      float drag,
      float turbulence,
      float waveAmplitude,
      float waveSpeed,
      int waveAxis,
      float dirX,
      float dirY,
      float dirZ,
      double playerX,
      double playerY,
      double playerZ,
      float attraction,
      float spiralSpeed,
      float spiralStrength
   ) {
      this.vx += dirX * 0.001;
      this.vy += dirY * 0.001;
      this.vz += dirZ * 0.001;
      this.vy -= gravity * 0.01;
      if (attraction != 0.0F) {
         double dx = playerX - this.x;
         double dy = playerY + 1.0 - this.y;
         double dz = playerZ - this.z;
         double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (dist > 0.5) {
            double force = attraction * 0.01 / Math.max(dist, 1.0);
            this.vx += dx * force;
            this.vy += dy * force;
            this.vz += dz * force;
         }
      }

      if (spiralStrength > 0.0F && spiralSpeed != 0.0F) {
         this.spiralAngle = (float)(this.spiralAngle + spiralSpeed * 0.1);
         this.spiralRadius = (float)(this.spiralRadius + spiralStrength * 0.01);
         double spiralX = Math.cos(this.spiralAngle) * this.spiralRadius * 0.02;
         double spiralZ = Math.sin(this.spiralAngle) * this.spiralRadius * 0.02;
         this.vx += spiralX;
         this.vz += spiralZ;
      }

      this.vx *= 1.0 - drag;
      this.vy *= 1.0 - drag;
      this.vz *= 1.0 - drag;
      if (turbulence > 0.0F) {
         this.vx = this.vx + (RANDOM.nextDouble() - 0.5) * turbulence * 0.1;
         this.vy = this.vy + (RANDOM.nextDouble() - 0.5) * turbulence * 0.1;
         this.vz = this.vz + (RANDOM.nextDouble() - 0.5) * turbulence * 0.1;
      }

      this.x = this.x + this.vx;
      this.y = this.y + this.vy;
      this.z = this.z + this.vz;
      if (waveAmplitude > 0.0F) {
         float waveOffset = (float)Math.sin(this.phase + (this.maxLifetime - this.lifetime) * waveSpeed * 0.1) * waveAmplitude * 0.1F;
         switch (waveAxis) {
            case 0:
               this.x += waveOffset;
               break;
            case 1:
               this.y += waveOffset;
               break;
            case 2:
               this.z += waveOffset;
         }
      }

      this.rotationY = this.rotationY + this.rotationSpeedY;
      this.rotationX = this.rotationX + this.rotationSpeedX;
      this.lifetime--;
   }

   public boolean isDead() {
      return this.lifetime <= 0;
   }

   public float getAlpha() {
      float progress = (float)this.lifetime / this.maxLifetime;
      if (progress > 0.8F) {
         return smooth((1.0F - progress) / 0.2F);
      } else {
         return progress < 0.3F ? smooth(progress / 0.3F) : 1.0F;
      }
   }

   private static float smooth(float value) {
      float t = Math.max(0.0F, Math.min(1.0F, value));
      return t * t * (3.0F - 2.0F * t);
   }

   public float getCurrentSize(boolean pulse, float pulseStrength, float pulseSpeed) {
      float progress = (float)this.lifetime / this.maxLifetime;
      float pulseValue = 1.0F;
      if (pulse) {
         pulseValue = (float)(1.0 + Math.sin(this.phase + (this.maxLifetime - this.lifetime) * pulseSpeed * 0.05) * pulseStrength * 0.3);
      }

      return this.initialSize * (0.5F + progress * 0.5F) * pulseValue;
   }

   public double getDistanceTo(double px, double py, double pz) {
      double dx = this.x - px;
      double dy = this.y - py;
      double dz = this.z - pz;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }
}
