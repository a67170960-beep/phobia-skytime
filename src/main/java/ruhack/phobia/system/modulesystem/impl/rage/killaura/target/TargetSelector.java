package ruhack.phobia.system.modulesystem.impl.rage.killaura.target;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.impl.rage.AntiBot;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.rotation.mod.PointFinder;
import ruhack.phobia.util.rotation.raycast.RaytracingUtil;

public class TargetSelector implements IMinecraft {
   private final PointFinder pointFinder = new PointFinder();
   private LivingEntity currentTarget = null;
   private Stream<LivingEntity> potentialTargets;

   public void lockTarget(LivingEntity target) {
      if (this.currentTarget == null) {
         this.currentTarget = target;
      }
   }

   public void releaseTarget() {
      this.currentTarget = null;
   }

   public void validateTarget(Predicate<LivingEntity> predicate) {
      this.findFirstMatch(predicate).ifPresent(this::lockTarget);
      if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
         this.releaseTarget();
      }
   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
      if (this.currentTarget != null
         && (
            !this.pointFinder.hasValidPoint(this.currentTarget, maxDistance, ignoreWalls) || this.getFov(this.currentTarget, maxDistance, ignoreWalls) > maxFov
         )) {
         this.releaseTarget();
      }

      this.potentialTargets = this.createStreamFromEntities(entities, maxDistance, maxFov, ignoreWalls);
   }

   private double getFov(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Vec3d attackVector = (Vec3d)this.pointFinder
         .computeVector(entity, maxDistance, RotationController.INSTANCE.getRotation(), new LinearSmoothMode().randomValue(), ignoreWalls)
         .getLeft();
      return RaytracingUtil.rayTrace(maxDistance, entity.getBoundingBox())
         ? 0.0
         : RotationController.computeRotationDifference(AngleUtil.cameraAngle(), AngleUtil.calculateAngle(attackVector));
   }

   private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
      return StreamSupport.stream(entities.spliterator(), false)
         .filter(LivingEntity.class::isInstance)
         .map(LivingEntity.class::cast)
         .filter(entity -> this.pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls) && this.getFov(entity, maxDistance, ignoreWalls) < maxFov)
         .sorted(Comparator.comparingDouble(entity -> entity.distanceTo(mc.player)));
   }

   private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
      return this.potentialTargets.filter(predicate).findFirst();
   }

   @Generated
   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   @Generated
   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   @Generated
   public Stream<LivingEntity> getPotentialTargets() {
      return this.potentialTargets;
   }

   public static class EntityFilter {
      private final List<String> targetSettings;

      public boolean isValid(LivingEntity entity) {
         if (this.isLocalPlayer(entity)) {
            return false;
         } else if (this.isInvalidHealth(entity)) {
            return false;
         } else {
            return this.isBotPlayer(entity) ? false : this.isValidEntityType(entity);
         }
      }

      private boolean isLocalPlayer(LivingEntity entity) {
         return entity == IMinecraft.mc.player;
      }

      private boolean isInvalidHealth(LivingEntity entity) {
         return !entity.isAlive() || entity.getHealth() <= 0.0F;
      }

      private boolean isBotPlayer(LivingEntity entity) {
         return entity instanceof PlayerEntity player && AntiBot.getInstance().isBot(player);
      }

      private boolean canAttackPlayer(PlayerEntity player) {
         boolean selectedType = FriendUtils.isFriend(player) ? this.targetSettings.contains("Друзья") : this.targetSettings.contains("Игроки");
         return selectedType && (this.targetSettings.contains("Голые") || this.hasArmor(player));
      }

      private boolean hasArmor(PlayerEntity player) {
         return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
            || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
      }

      private boolean isValidEntityType(LivingEntity entity) {
         return switch (entity) {
            case PlayerEntity player -> this.canAttackPlayer(player);
            case AnimalEntity animal -> this.targetSettings.contains("Мобы");
            case MobEntity mob -> this.targetSettings.contains("Мобы");
            default -> false;
         };
      }

      @Generated
      public EntityFilter(List<String> targetSettings) {
         this.targetSettings = targetSettings;
      }
   }
}
