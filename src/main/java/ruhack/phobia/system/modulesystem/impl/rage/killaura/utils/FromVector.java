package ruhack.phobia.system.modulesystem.impl.rage.killaura.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.util.timer.StopWatch;

public class FromVector implements IMinecraft {
   private static final Random random = new Random();
   private static final StopWatch pointTimer = new StopWatch();
   private static final StopWatch updateTimer = new StopWatch();
   private static List<Vec3d> cachedOffsets = new ArrayList<>();
   private static int currentPointIndex = 0;
   private static Vec3d lastShouldPoint = Vec3d.ZERO;
   private static final Random shouldRandom = new Random();
   private static Vec3d lastOffset = Vec3d.ZERO;

   public static Vec3d shouldAfterAttack(Entity entity, boolean shouldGenerateNew) {
      if (shouldGenerateNew || lastOffset.equals(Vec3d.ZERO)) {
         lastOffset = generateRandomOffset(entity);
      }

      return lastOffset;
   }

   private static Vec3d generateRandomOffset(Entity entity) {
      double w = entity.getWidth();
      double h = entity.getHeight();
      double margin = 0.18;
      double localX = shouldRandom.nextDouble(-w / 2.0 + margin, w / 2.0 - margin);
      double localY = shouldRandom.nextDouble(margin, h - margin);
      double localZ = shouldRandom.nextDouble(-w / 2.0 + margin, w / 2.0 - margin);
      return new Vec3d(localX, localY, localZ);
   }

   public static void resetShouldAfterAttack() {
      lastOffset = Vec3d.ZERO;
   }

   public static Vec3d hitbox(Entity entity, float X, float Y, float Z, float WIDTH) {
      double wHalf = entity.getWidth() / WIDTH;
      double yExpand = MathHelper.clamp(entity.getEyeY() - entity.getY(), 0.0, entity.getHeight());
      double xExpand = MathHelper.clamp(mc.player.getX() - entity.getX(), -wHalf, wHalf);
      double zExpand = MathHelper.clamp(mc.player.getZ() - entity.getZ(), -wHalf, wHalf);
      return new Vec3d(entity.getX() + xExpand / X, entity.getY() + yExpand / Y, entity.getZ() + zExpand / Z);
   }

   public static Vec3d spiralPredict(Entity entity, float radius, float speed, float switchDelay) {
      if (entity == null) {
         return Vec3d.ZERO;
      }

      Vec3d velocity = entity.getVelocity();
      Vec3d predictedPos = entity.getEntityPos().add(velocity.multiply(0.5));
      long time = (long)((float)System.currentTimeMillis() % (switchDelay * 1000.0F));
      double angle = (float)time * speed * Math.PI / 180.0;
      double spiralX = Math.cos(angle) * radius;
      double spiralZ = Math.sin(angle) * radius;
      double spiralY = random.nextDouble(0.4, 0.7) * entity.getHeight();
      return new Vec3d(predictedPos.x + spiralX, predictedPos.y + spiralY, predictedPos.z + spiralZ);
   }

   public static Vec3d predict(Entity entity, float predictionTicks) {
      if (entity == null) {
         return Vec3d.ZERO;
      }

      Vec3d velocity = entity.getVelocity();
      Vec3d currentPos = entity.getEntityPos();
      Vec3d predictedPos = currentPos.add(velocity.x * predictionTicks, velocity.y * predictionTicks * 0.8, velocity.z * predictionTicks);
      double safeMargin = 0.05;
      return new Vec3d(
         MathHelper.clamp(predictedPos.x, entity.getBoundingBox().minX + safeMargin, entity.getBoundingBox().maxX - safeMargin),
         MathHelper.clamp(predictedPos.y, entity.getBoundingBox().minY + safeMargin, entity.getBoundingBox().maxY - safeMargin),
         MathHelper.clamp(predictedPos.z, entity.getBoundingBox().minZ + safeMargin, entity.getBoundingBox().maxZ - safeMargin)
      );
   }

   public static Vec3d closest(Entity entity) {
      if (entity == null) {
         return Vec3d.ZERO;
      }

      Vec3d playerEyes = mc.player.getEyePos();
      double minX = entity.getBoundingBox().minX;
      double minY = entity.getBoundingBox().minY;
      double minZ = entity.getBoundingBox().minZ;
      double maxX = entity.getBoundingBox().maxX;
      double maxY = entity.getBoundingBox().maxY;
      double maxZ = entity.getBoundingBox().maxZ;
      double closestX = MathHelper.clamp(playerEyes.x, minX, maxX);
      double closestY = MathHelper.clamp(playerEyes.y, minY, maxY);
      double closestZ = MathHelper.clamp(playerEyes.z, minZ, maxZ);
      return new Vec3d(closestX, closestY, closestZ);
   }

   public static Vec3d getBestPoint(Vec3d pos, Entity entity) {
      if (entity == null) {
         return Vec3d.ZERO;
      }

      double safePoint = 0.0;
      return new Vec3d(
         MathHelper.clamp(pos.x, entity.getBoundingBox().minX + safePoint, entity.getBoundingBox().maxX - safePoint),
         MathHelper.clamp(pos.y, entity.getBoundingBox().minY + safePoint, entity.getBoundingBox().maxY - safePoint),
         MathHelper.clamp(pos.z, entity.getBoundingBox().minZ + safePoint, entity.getBoundingBox().maxZ - safePoint)
      );
   }

   public static Vec3d brain(Entity entity, float min, float max) {
      double distance = mc.player.getEntityPos().distanceTo(entity.getEntityPos());
      double normalizedDistance = MathHelper.clamp((distance - min) / (max - min), 0.0, 1.0);
      double heightFactor = normalizedDistance;
      double minHeight = 0.2;
      double maxHeight = 0.8;
      double targetHeight = minHeight + (maxHeight - minHeight) * heightFactor;
      double targetY = entity.getY() + entity.getHeight() * targetHeight;
      return new Vec3d(entity.getX(), targetY, entity.getZ());
   }

   public static Vec3d custom(Entity entity, int pointCount, float switchDelay) {
      if (entity == null) {
         return Vec3d.ZERO;
      }

      if (updateTimer.every(1000.0) || cachedOffsets.isEmpty()) {
         generateRandomPoints(entity, pointCount);
         currentPointIndex = 0;
         pointTimer.reset();
      }

      if (pointTimer.finished(switchDelay)) {
         currentPointIndex = (currentPointIndex + 1) % cachedOffsets.size();
         pointTimer.reset();
      }

      if (cachedOffsets.isEmpty()) {
         return entity.getEntityPos();
      }

      Vec3d currentPos = entity.getEntityPos();
      Vec3d offset = cachedOffsets.get(currentPointIndex);
      return currentPos.add(offset);
   }

   private static void generateRandomPoints(Entity entity, int pointCount) {
      cachedOffsets.clear();
      double width = entity.getWidth();
      double height = entity.getHeight();

      for (int i = 0; i < pointCount; i++) {
         double offsetX = (random.nextDouble() - 0.5) * width;
         double offsetY = random.nextDouble() * height;
         double offsetZ = (random.nextDouble() - 0.5) * width;
         cachedOffsets.add(new Vec3d(offsetX, offsetY, offsetZ));
      }
   }

   public static List<Vec3d> getAllCachedPoints() {
      if (mc.targetedEntity == null) {
         return new ArrayList<>();
      }

      Vec3d pos = mc.targetedEntity.getEntityPos();
      List<Vec3d> absolute = new ArrayList<>();

      for (Vec3d offset : cachedOffsets) {
         absolute.add(pos.add(offset));
      }

      return absolute;
   }

   public static void clearCache() {
      cachedOffsets.clear();
      currentPointIndex = 0;
      pointTimer.reset();
      updateTimer.reset();
   }

   public static void forceUpdate(Entity entity, int pointCount) {
      generateRandomPoints(entity, pointCount);
      currentPointIndex = 0;
      pointTimer.reset();
      updateTimer.reset();
   }
}
