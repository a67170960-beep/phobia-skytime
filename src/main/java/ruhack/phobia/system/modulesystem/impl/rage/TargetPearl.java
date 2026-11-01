package ruhack.phobia.system.modulesystem.impl.rage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.player.swap.InventoryUtils;

public class TargetPearl extends ModuleStructure {
   private static final int MAX_SIMULATION_TICKS = 160;
   private static final double MAX_TARGET_ERROR = 3.0;
   private static final float PEARL_SPEED = 1.5F;
   private static final double PEARL_GRAVITY = 0.03;
   private static final double PEARL_DRAG = 0.99;
   private static final double MIN_FOLLOW_OFFSET = 1.5;
   private static final int SAFE_SEARCH_RADIUS = 4;
   private static final int MAX_SAFE_TARGETS = 4;
   private final SliderSettings distance = new SliderSettings("Мин. дистанция", "Минимальная дальность приземления", 10.0F).range(8.0F, 20.0F).step(1.0F);
   private final BooleanSetting ignoreFriends = new BooleanSetting("Игнорировать друзей", "Не повторять жемчуг друга").setValue(true);
   private long lastThrow;
   private long lastScan;

   public TargetPearl() {
      super("TargetPearl", "Бросает жемчуг к прогнозируемой точке приземления чужого жемчуга", ModuleCategory.RAGE);
      this.settings(this.ignoreFriends, this.distance);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      long now = System.currentTimeMillis();
      if (mc.player != null
         && mc.world != null
         && !mc.player.isGliding()
         && now - this.lastThrow >= 2000L
         && now - this.lastScan >= 70L
         && !mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())
         && InventoryUtils.findItemAnywhere(Items.ENDER_PEARL) != -1) {
         this.lastScan = now;
         Vec3d bestLanding = null;
         double bestDistance = Double.MAX_VALUE;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity pearl
               && pearl.getOwner() != mc.player
               && (!this.ignoreFriends.isValue() || pearl.getOwner() == null || !FriendUtils.isFriend(pearl.getOwner()))) {
               Vec3d landing = this.predictLanding(pearl);
               if (landing != null) {
                  double d = mc.player.getEntityPos().distanceTo(landing);
                  if (d >= this.distance.getValue() && d <= 120.0 && d < bestDistance) {
                     bestDistance = d;
                     bestLanding = landing;
                  }
               }
            }
         }

         if (bestLanding != null) {
            TargetPearl.ThrowSolution solution = this.findBestThrowSolution(bestLanding);
            if (solution != null && !(solution.error() > 3.0)) {
               float cameraYaw = mc.player.getYaw();
               float cameraPitch = mc.player.getPitch();
               boolean onGround = mc.player.isOnGround();
               boolean horizontalCollision = mc.player.horizontalCollision;
               mc.getNetworkHandler().sendPacket(new LookAndOnGround(solution.yaw(), solution.pitch(), onGround, horizontalCollision));
               InventoryUtils.silentUseItem(Items.ENDER_PEARL, solution.yaw(), solution.pitch());
               mc.getNetworkHandler().sendPacket(new LookAndOnGround(cameraYaw, cameraPitch, onGround, horizontalCollision));
               this.lastThrow = now;
            }
         }
      }
   }

   private Vec3d predictLanding(EnderPearlEntity pearl) {
      Vec3d position = pearl.getEntityPos();
      Vec3d velocity = pearl.getVelocity();

      for (int tick = 0; tick < 160; tick++) {
         velocity = velocity.add(0.0, -0.03, 0.0).multiply(0.99);
         Vec3d next = position.add(velocity);
         BlockHitResult collision = this.raycast(position, next, pearl);
         if (collision != null) {
            return collision.getPos();
         }

         if (next.y <= mc.world.getBottomY()) {
            return next;
         }

         position = next;
      }

      return null;
   }

   private TargetPearl.ThrowSolution findBestThrowSolution(Vec3d impact) {
      TargetPearl.ThrowSolution best = null;

      for (Vec3d target : this.findSafeTargets(impact)) {
         TargetPearl.ThrowSolution solution = this.findThrowSolution(target);
         if (solution != null && (best == null || solution.ticks() < best.ticks() || solution.ticks() == best.ticks() && solution.error() < best.error())) {
            best = solution;
         }
      }

      return best;
   }

   private TargetPearl.ThrowSolution findThrowSolution(Vec3d target) {
      Vec3d eye = this.getThrowPosition();
      double dx = target.x - eye.x;
      double dz = target.z - eye.z;
      float yaw = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
      TargetPearl.ThrowSolution best = null;

      for (float pitch = -55.0F; pitch <= 80.0F; pitch += 0.5F) {
         TargetPearl.TrajectoryResult result = this.simulateOwnPearl(yaw, pitch);
         if (result != null) {
            double error = result.landing().distanceTo(target);
            if (!(error > 3.0) && (best == null || result.ticks() < best.ticks() || result.ticks() == best.ticks() && error < best.error())) {
               best = new TargetPearl.ThrowSolution(yaw, pitch, error, result.ticks());
            }
         }
      }

      return best;
   }

   private List<Vec3d> findSafeTargets(Vec3d impact) {
      BlockPos center = BlockPos.ofFloored(impact);
      List<TargetPearl.LandingCandidate> candidates = new ArrayList<>();

      for (int dx = -4; dx <= 4; dx++) {
         for (int dz = -4; dz <= 4; dz++) {
            double horizontalOffset = Math.hypot(dx, dz);
            if (!(horizontalOffset < 1.5) && !(horizontalOffset > 4.0)) {
               for (int y = center.getY() + 4; y >= center.getY() - 6; y--) {
                  BlockPos floor = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                  if (this.isSafeLandingFloor(floor)) {
                     Vec3d point = new Vec3d(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5);
                     double score = point.squaredDistanceTo(impact) + horizontalOffset * 0.35;
                     candidates.add(new TargetPearl.LandingCandidate(point, score));
                     break;
                  }
               }
            }
         }
      }

      candidates.sort(Comparator.comparingDouble(TargetPearl.LandingCandidate::score));
      return candidates.stream().limit(4L).map(TargetPearl.LandingCandidate::position).toList();
   }

   private boolean isSafeLandingFloor(BlockPos floor) {
      BlockState floorState = mc.world.getBlockState(floor);
      if (!floorState.getCollisionShape(mc.world, floor).isEmpty() && !floorState.isIn(BlockTags.LOGS) && !floorState.isIn(BlockTags.LEAVES)) {
         BlockPos feet = floor.up();
         BlockPos head = feet.up();
         return mc.world.getBlockState(feet).getCollisionShape(mc.world, feet).isEmpty()
            && mc.world.getBlockState(head).getCollisionShape(mc.world, head).isEmpty()
            && mc.world.getFluidState(feet).isEmpty()
            && mc.world.getFluidState(head).isEmpty();
      } else {
         return false;
      }
   }

   private TargetPearl.TrajectoryResult simulateOwnPearl(float yaw, float pitch) {
      Vec3d position = this.getThrowPosition();
      Vec3d velocity = this.getThrowVelocity(yaw, pitch);

      for (int tick = 0; tick < 160; tick++) {
         velocity = velocity.add(0.0, -0.03, 0.0).multiply(0.99);
         Vec3d next = position.add(velocity);
         BlockHitResult collision = this.raycast(position, next, mc.player);
         if (collision != null) {
            if (collision.getSide() == Direction.UP && this.isSafeLandingFloor(collision.getBlockPos())) {
               return new TargetPearl.TrajectoryResult(collision.getPos(), tick + 1);
            }

            return null;
         }

         if (next.y <= mc.world.getBottomY()) {
            return new TargetPearl.TrajectoryResult(next, tick + 1);
         }

         position = next;
      }

      return null;
   }

   private Vec3d getThrowPosition() {
      return new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
   }

   private Vec3d getThrowVelocity(float yaw, float pitch) {
      double yawRadians = Math.toRadians(yaw);
      double pitchRadians = Math.toRadians(pitch);
      double horizontal = Math.cos(pitchRadians) * 1.5;
      Vec3d playerMovement = mc.player.getMovement();
      return new Vec3d(
         -Math.sin(yawRadians) * horizontal + playerMovement.x,
         -Math.sin(pitchRadians) * 1.5 + (mc.player.isOnGround() ? 0.0 : playerMovement.y),
         Math.cos(yawRadians) * horizontal + playerMovement.z
      );
   }

   private BlockHitResult raycast(Vec3d start, Vec3d end, Entity source) {
      BlockHitResult hit = mc.world.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, source));
      return hit.getType() == Type.MISS ? null : hit;
   }

   private record LandingCandidate(Vec3d position, double score) {
   }

   private record ThrowSolution(float yaw, float pitch, double error, int ticks) {
   }

   private record TrajectoryResult(Vec3d landing, int ticks) {
   }
}
