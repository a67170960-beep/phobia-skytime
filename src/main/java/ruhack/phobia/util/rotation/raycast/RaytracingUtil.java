package ruhack.phobia.util.rotation.raycast;

import java.util.Objects;
import java.util.function.Predicate;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ruhack.phobia.IMinecraft;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.attack.AttackPerpetrator;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;

public final class RaytracingUtil implements IMinecraft {
   public static BlockHitResult raycast(double range, Angle angle, boolean includeFluids) {
      return raycast(Objects.requireNonNull(mc.player).getCameraPosVec(1.0F), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3d vec, double range, Angle angle, boolean includeFluids) {
      Entity entity = mc.getCameraEntity();
      if (entity == null) {
         return null;
      }

      Vec3d rotationVec = angle.toVector();
      Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
      World world = mc.world;
      if (world == null) {
         return null;
      }

      FluidHandling fluidHandling = includeFluids ? FluidHandling.ANY : FluidHandling.NONE;
      RaycastContext context = new RaycastContext(vec, end, ShapeType.OUTLINE, fluidHandling, entity);
      return world.raycast(context);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
      return raycast(start, end, shapeType, mc.player);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, Entity entity) {
      return raycast(start, end, shapeType, FluidHandling.NONE, entity);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, FluidHandling fluidHandling, Entity entity) {
      return mc.world.raycast(new RaycastContext(start, end, shapeType, fluidHandling, entity));
   }

   public static EntityHitResult raytraceEntity(double range, Angle angle, Predicate<Entity> filter) {
      Entity entity = mc.player;
      if (entity == null) {
         return null;
      }

      Vec3d cameraVec = entity.getCameraPosVec(1.0F);
      Vec3d rotationVec = angle.toVector();
      Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
      Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0);
      return ProjectileUtil.raycast(entity, cameraVec, vec3d3, box, e -> !e.isSpectator() && filter.test(e), range * range);
   }

   public static boolean rayTrace(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      return rayTrace(RotationController.INSTANCE.getRotation().toVector(), config.getMaximumRange(), config.getBox());
   }

   public static boolean rayTrace(double range, Box box) {
      return rayTrace(RotationController.INSTANCE.getRotation().toVector(), range, box);
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
      Vec3d cameraVec = Objects.requireNonNull(mc.player).getEyePos();
      return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
   }

   @Generated
   private RaytracingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
