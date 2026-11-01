package ruhack.phobia.util.aim.neuro;

import com.mojang.authlib.GameProfile;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FakePlayerSpar {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public static final int ENTITY_ID = 2147479405;
   private static final double WALK_ACCEL = 0.1;
   private static final double SPRINT_ACCEL = 0.13;
   private static final double AIR_ACCEL = 0.02;
   private static final double GROUND_FRICTION = 0.546;
   private static final double AIR_DRAG = 0.91;
   private static final double GRAVITY = 0.08;
   private static final double VERTICAL_DRAG = 0.98;
   private static final double JUMP_VELOCITY = 0.42;
   private static final double SPRINT_JUMP_BOOST = 0.2;
   private static final double MAX_HORIZONTAL_SPEED = 0.6;
   private static final double TELEPORT_DISTANCE = 12.0;
   private static final double SPAWN_DISTANCE = 2.8;
   private static final int TEMPO_CALM = 0;
   private static final int TEMPO_NORMAL = 1;
   private static final int TEMPO_RUSH = 2;
   private final Random random = new Random();
   private OtherClientPlayerEntity fake;
   private String name;
   private Vec3d velocity = Vec3d.ZERO;
   private boolean onGround;
   private boolean sprinting = true;
   private boolean clinch;
   private int strafeDir = 1;
   private double targetDist = 2.8;
   private int strafeTimer;
   private int distTimer;
   private int jumpTimer;
   private int sprintTimer;
   private int pauseTimer;
   private int swingTimer;
   private int tempo = 1;
   private int tempoTimer;
   private int dashTicks;

   public boolean isActive() {
      return this.fake != null;
   }

   public OtherClientPlayerEntity getFake() {
      return this.fake;
   }

   public boolean isFakeId(int entityId) {
      return this.fake != null && entityId == 2147479405;
   }

   public void spawn(String name) {
      if (mc.world != null && mc.player != null) {
         this.despawn();
         this.name = name != null ? name : "Sparring";
         this.velocity = Vec3d.ZERO;
         this.onGround = false;
         this.sprinting = true;
         this.clinch = false;
         this.strafeDir = this.random.nextBoolean() ? 1 : -1;
         this.targetDist = 2.4 + this.random.nextDouble() * 1.2;
         this.strafeTimer = 10 + this.random.nextInt(30);
         this.distTimer = 40 + this.random.nextInt(50);
         this.jumpTimer = 25 + this.random.nextInt(45);
         this.sprintTimer = 30 + this.random.nextInt(50);
         this.pauseTimer = 0;
         this.swingTimer = 20 + this.random.nextInt(40);
         this.tempo = 1;
         this.tempoTimer = 100 + this.random.nextInt(200);
         this.dashTicks = 0;
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         Vec3d pos = mc.player.getEntityPos().add(Math.cos(angle) * 2.8, 0.1, Math.sin(angle) * 2.8);
         this.createFake(pos, mc.player.getYaw() + 180.0F);
      }
   }

   public void despawn() {
      if (this.fake != null && mc.world != null) {
         mc.world.removeEntity(this.fake.getId(), RemovalReason.DISCARDED);
      }

      this.fake = null;
   }

   public void tick() {
      if (this.fake != null && mc.player != null && mc.world != null) {
         this.ensureAlive();
         if (this.fake != null) {
            if (this.fake.getEntityPos().distanceTo(mc.player.getEntityPos()) > 12.0) {
               double angle = this.random.nextDouble() * Math.PI * 2.0;
               Vec3d pos = mc.player.getEntityPos().add(Math.cos(angle) * 2.8, 0.1, Math.sin(angle) * 2.8);
               this.fake.setPos(pos.x, pos.y, pos.z);
               this.velocity = Vec3d.ZERO;
               this.onGround = false;
            }

            this.fake.setHealth(this.fake.getMaxHealth());
            this.fake.setNoGravity(true);
            this.simulateMovement();
            this.lookAtPlayer();
            if (--this.swingTimer <= 0) {
               this.swingTimer = 15 + this.random.nextInt(45);
               if (this.fake.distanceTo(mc.player) < 3.5) {
                  this.fake.swingHand(Hand.MAIN_HAND);
               }
            }
         }
      }
   }

   private void ensureAlive() {
      boolean broken = this.fake.isRemoved() || this.fake.getEntityWorld() != mc.world || mc.world.getEntityById(2147479405) != this.fake;
      if (broken) {
         Vec3d pos = this.fake.getEntityPos();
         float yaw = this.fake.getYaw();
         if (pos.distanceTo(mc.player.getEntityPos()) > 12.0) {
            pos = mc.player.getEntityPos().add(2.8, 0.1, 0.0);
         }

         this.createFake(pos, yaw);
      }
   }

   private void createFake(Vec3d pos, float yaw) {
      GameProfile profile = new GameProfile(UUID.randomUUID(), this.name != null ? this.name : "Sparring");
      OtherClientPlayerEntity e = new OtherClientPlayerEntity(mc.world, profile);
      e.setId(2147479405);
      e.setNoGravity(true);
      e.noClip = false;
      e.setHealth(e.getMaxHealth());
      e.setPos(pos.x, pos.y, pos.z);
      e.setYaw(yaw);
      e.setHeadYaw(yaw);
      e.setBodyYaw(yaw);
      this.fake = e;
      mc.world.addEntity(e);
   }

   private void simulateMovement() {
      Vec3d playerPos = mc.player.getEntityPos();
      Vec3d fakePos = this.fake.getEntityPos();
      if (--this.tempoTimer <= 0) {
         int r = this.random.nextInt(20);
         this.tempo = r < 10 ? 0 : (r < 17 ? 1 : 2);
         this.tempoTimer = 160 + this.random.nextInt(240);
         this.dashTicks = 0;
      }

      if (--this.strafeTimer <= 0) {
         this.strafeDir = -this.strafeDir;
         this.strafeTimer = !this.clinch && this.tempo != 2
            ? (this.tempo == 0 ? 30 + this.random.nextInt(50) : 10 + this.random.nextInt(30))
            : 6 + this.random.nextInt(14);
      }

      if (--this.distTimer <= 0) {
         int clinchChance = this.tempo == 2 ? 4 : (this.tempo == 0 ? 0 : 2);
         this.clinch = clinchChance > 0 && this.random.nextInt(10) < clinchChance;
         if (this.clinch) {
            this.targetDist = 0.5 + this.random.nextDouble() * 0.6;
            this.distTimer = 24 + this.random.nextInt(30);
         } else if (this.tempo == 0) {
            this.targetDist = 4.5 + this.random.nextDouble() * 2.5;
            this.distTimer = 70 + this.random.nextInt(70);
         } else if (this.tempo == 2) {
            this.targetDist = 2.0 + this.random.nextDouble() * 1.5;
            this.distTimer = 40 + this.random.nextInt(40);
         } else {
            this.targetDist = 3.5 + this.random.nextDouble() * 1.8;
            this.distTimer = 50 + this.random.nextInt(50);
         }

         if (this.tempo == 2 && !this.clinch && this.random.nextInt(10) < 2) {
            this.dashTicks = 14 + this.random.nextInt(10);
         }
      }

      if (--this.sprintTimer <= 0) {
         this.sprinting = this.tempo == 2 || (this.tempo == 0 ? this.random.nextInt(4) == 0 : this.random.nextInt(4) != 0);
         this.sprintTimer = this.sprinting ? 30 + this.random.nextInt(50) : 6 + this.random.nextInt(10);
      }

      boolean paused = this.pauseTimer > 0;
      if (paused) {
         this.pauseTimer--;
      } else if (this.random.nextInt(this.tempo == 0 ? 70 : (this.tempo == 2 ? 320 : 160)) == 0) {
         this.pauseTimer = this.tempo == 0 ? 4 + this.random.nextInt(10) : 2 + this.random.nextInt(5);
      }

      Vec3d toFake = new Vec3d(fakePos.x - playerPos.x, 0.0, fakePos.z - playerPos.z);
      double dist = toFake.length();
      Vec3d intent = Vec3d.ZERO;
      if (this.dashTicks > 0 && !paused) {
         this.dashTicks--;
         this.sprinting = true;
         intent = dist > 0.05 ? toFake.multiply(-1.0 / dist) : new Vec3d(1.0, 0.0, 0.0);
      } else if (!paused) {
         Vec3d radial = dist > 0.05 ? toFake.multiply(1.0 / dist) : new Vec3d(1.0, 0.0, 0.0);
         Vec3d tangent = new Vec3d(-radial.z, 0.0, radial.x).multiply(this.strafeDir);
         double band = this.clinch ? 0.15 : 0.5;
         double approach = this.clinch ? -1.8 : -0.9;
         double radialWeight = 0.0;
         if (dist > this.targetDist + band) {
            radialWeight = approach;
         } else if (dist < this.targetDist - band) {
            radialWeight = 0.9;
         }

         intent = tangent.multiply(this.clinch ? 0.55 : 1.0).add(radial.multiply(radialWeight));
         double len = intent.length();
         if (len > 0.01) {
            intent = intent.multiply(1.0 / len);
         }
      }

      boolean jump = false;
      if (this.onGround && !paused) {
         if (--this.jumpTimer <= 0) {
            this.jumpTimer = 25 + this.random.nextInt(45);
            jump = true;
         } else if (this.fake.horizontalCollision) {
            jump = true;
         }
      }

      double accel = this.onGround ? (this.sprinting ? 0.13 : 0.1) : 0.02 * (this.sprinting ? 1.3 : 1.0);
      this.velocity = this.velocity.add(intent.multiply(accel * 0.98));
      if (jump) {
         this.velocity = new Vec3d(this.velocity.x, 0.42, this.velocity.z);
         if (this.sprinting && intent.lengthSquared() > 0.01) {
            this.velocity = this.velocity.add(intent.multiply(0.2));
         }
      }

      double horizontal = Math.hypot(this.velocity.x, this.velocity.z);
      if (horizontal > 0.6) {
         double scale = 0.6 / horizontal;
         this.velocity = new Vec3d(this.velocity.x * scale, this.velocity.y, this.velocity.z * scale);
      }

      this.fake.setSprinting(this.sprinting && !paused && this.onGround);
      this.fake.move(MovementType.SELF, this.velocity);
      this.onGround = this.fake.isOnGround();
      if (this.fake.horizontalCollision && this.onGround && this.random.nextInt(3) == 0) {
         this.strafeDir = -this.strafeDir;
         this.strafeTimer = 10 + this.random.nextInt(30);
      }

      double drag = this.onGround ? 0.546 : 0.91;
      double vy;
      if (this.onGround) {
         vy = 0.0;
      } else if (this.fake.verticalCollision && this.velocity.y > 0.0) {
         vy = 0.0;
      } else {
         vy = (this.velocity.y - 0.08) * 0.98;
      }

      this.velocity = new Vec3d(this.velocity.x * drag, vy, this.velocity.z * drag);
   }

   private void lookAtPlayer() {
      Vec3d toPlayer = mc.player.getEyePos().subtract(this.fake.getEyePos());
      float yaw = (float)Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(toPlayer.y, Math.hypot(toPlayer.x, toPlayer.z))));
      this.fake.setYaw(yaw);
      this.fake.setHeadYaw(yaw);
      this.fake.setBodyYaw(yaw);
      this.fake.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
   }
}
