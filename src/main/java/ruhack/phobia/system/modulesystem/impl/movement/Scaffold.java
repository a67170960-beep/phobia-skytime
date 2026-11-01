package ruhack.phobia.system.modulesystem.impl.movement;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.packet.AdminSmooth;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;

public class Scaffold extends ModuleStructure {
   private long lastRightClickTime = 0L;
   private long enableTimeMs = 0L;
   private float stableYaw = 180.0F;
   private int oldSlot = -1;
   private int lockedBlockSlot = -1;

   public Scaffold() {
      super("Scaffold", "Advanced Scaffold", ModuleCategory.MOVEMENT);
   }

   @Override
   public void activate() {
      super.activate();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         this.oldSlot = mc.player.getInventory().getSelectedSlot();
         this.stableYaw = this.getMovementBasedYaw();
         this.lockedBlockSlot = -1;
         this.lastRightClickTime = 0L;
         this.enableTimeMs = System.currentTimeMillis();
      }
   }

   @Override
   public void deactivate() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.oldSlot);
         mc.options.sneakKey.setPressed(false);
      }

      this.oldSlot = -1;
      this.lockedBlockSlot = -1;
      this.lastRightClickTime = 0L;
      this.enableTimeMs = 0L;
      super.deactivate();
   }

   @EventHandler
   public void onUpdate(TickEvent event) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         this.stableYaw = this.getMovementBasedYaw();
         float targetYaw = this.stableYaw;
         float targetPitch = this.calculateDynamicPitch();
         RotationController.INSTANCE
            .rotateTo(new Angle(targetYaw, targetPitch), new RotationConfig(new AdminSmooth(), true, true, false), TaskPriority.LOW_PRIORITY, this);
         this.updateSneakState();
         this.tryPlaceLegit();
      }
   }

   private void updateSneakState() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.1, mc.player.getZ());
         boolean onGround = !mc.world.getBlockState(feet).isAir();
         if (onGround) {
            mc.options.sneakKey.setPressed(false);
         } else {
            double localX = mc.player.getX() - feet.getX();
            double localZ = mc.player.getZ() - feet.getZ();
            double minToEdge = Math.min(Math.min(localX, 1.0 - localX), Math.min(localZ, 1.0 - localZ));
            mc.options.sneakKey.setPressed(minToEdge < 0.3);
         }
      }
   }

   private void tryPlaceLegit() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.interactionManager != null && mc.player != null) {
         if (System.currentTimeMillis() - this.enableTimeMs >= 180L) {
            if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR && !mc.player.isUsingItem()) {
               HitResult crosshair = mc.crosshairTarget;
               if (crosshair instanceof BlockHitResult hitResult && crosshair.getType() == Type.BLOCK) {
                  if (this.isValidPlaceRaycast(hitResult)) {
                     int slot = this.getBlockSlot();
                     if (slot != -1) {
                        if (this.oldSlot == -1) {
                           this.oldSlot = mc.player.getInventory().getSelectedSlot();
                        }

                        mc.player.getInventory().setSelectedSlot(slot);
                        KeyBinding.onKeyPressed(mc.options.useKey.getDefaultKey());
                        mc.player.swingHand(Hand.MAIN_HAND);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isValidPlaceRaycast(BlockHitResult hitResult) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && mc.world != null) {
         BlockPos supportPos = hitResult.getBlockPos();
         if (mc.world.getBlockState(supportPos).isAir()) {
            return false;
         }

         if (!mc.world.getFluidState(supportPos).isEmpty()) {
            return false;
         }

         BlockPos placePos = supportPos.offset(hitResult.getSide());
         if (!mc.world.getBlockState(placePos).isReplaceable()) {
            return false;
         }

         BlockHitResult trace = mc.world
            .raycast(new RaycastContext(mc.player.getEyePos(), hitResult.getPos(), ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (trace.getType() != Type.BLOCK) {
            return true;
         }

         BlockPos traced = trace.getBlockPos();
         return traced.equals(supportPos) || traced.equals(placePos);
      } else {
         return false;
      }
   }

   private int getBlockSlot() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return -1;
      }

      if (this.isValidBlockSlot(this.lockedBlockSlot)) {
         return this.lockedBlockSlot;
      }

      int selectedSlot = mc.player.getInventory().getSelectedSlot();
      if (this.isValidBlockSlot(selectedSlot)) {
         this.lockedBlockSlot = selectedSlot;
         return this.lockedBlockSlot;
      }

      int bestSlot = -1;
      int bestCount = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof BlockItem && stack.getCount() > bestCount) {
            bestCount = stack.getCount();
            bestSlot = i;
         }
      }

      this.lockedBlockSlot = bestSlot;
      return bestSlot;
   }

   private boolean isValidBlockSlot(int slot) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (slot < 0 || slot > 8) {
         return false;
      }

      if (mc.player == null) {
         return false;
      }

      ItemStack stack = mc.player.getInventory().getStack(slot);
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
   }

   private float calculateDynamicPitch() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return 80.0F;
      } else {
         BlockPos standing = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
         double localX = mc.player.getX() - standing.getX();
         double localZ = mc.player.getZ() - standing.getZ();
         double west = localX;
         double east = 1.0 - localX;
         double north = localZ;
         double south = 1.0 - localZ;
         double min = Math.min(Math.min(west, east), Math.min(north, south));
         float basePitch = 78.0F;
         if (min < 0.2) {
            float pitchAdjustment = (float)((0.2 - min) * 15.0);
            return MathHelper.clamp(basePitch + pitchAdjustment, 72.0F, 86.0F);
         } else {
            return MathHelper.clamp(basePitch, -89.0F, 89.0F);
         }
      }
   }

   private float getTargetYaw() {
      return this.stableYaw;
   }

   private float getMovementBasedYaw() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return 180.0F;
      } else {
         Vec3d movementInput = this.getMovementInput();
         if (movementInput.lengthSquared() < 0.01) {
            return this.stableYaw;
         } else {
            double moveX = movementInput.x;
            double moveZ = movementInput.z;
            if (Math.abs(moveX) > Math.abs(moveZ)) {
               return moveX > 0.0 ? 90.0F : -90.0F;
            } else {
               return moveZ > 0.0 ? -180.0F : 0.0F;
            }
         }
      }
   }

   private Vec3d getMovementInput() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return Vec3d.ZERO;
      }

      float forward = 0.0F;
      float sideways = 0.0F;
      if (mc.options.forwardKey.isPressed()) {
         forward++;
      }

      if (mc.options.backKey.isPressed()) {
         forward--;
      }

      if (mc.options.leftKey.isPressed()) {
         sideways++;
      }

      if (mc.options.rightKey.isPressed()) {
         sideways--;
      }

      float yawRad = mc.player.getYaw() * (float) (Math.PI / 180.0);
      double sin = Math.sin(yawRad);
      double cos = Math.cos(yawRad);
      double moveX = sideways * cos - forward * sin;
      double moveZ = forward * cos + sideways * sin;
      return new Vec3d(moveX, 0.0, moveZ);
   }

   private float resolveEdgeBasedYaw(boolean holdCenter) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return 180.0F;
      }

      BlockPos standing = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
      double localX = mc.player.getX() - standing.getX();
      double localZ = mc.player.getZ() - standing.getZ();
      double west = localX;
      double east = 1.0 - localX;
      double north = localZ;
      double south = 1.0 - localZ;
      double min = west;
      Direction edge = Direction.WEST;
      if (east < min) {
         min = east;
         edge = Direction.EAST;
      }

      if (north < min) {
         min = north;
         edge = Direction.NORTH;
      }

      if (south < min) {
         min = south;
         edge = Direction.SOUTH;
      }

      if (holdCenter && min > 0.26) {
         return this.stableYaw;
      }

      return switch (edge) {
         case NORTH -> 180.0F;
         case SOUTH -> 0.0F;
         case WEST -> 90.0F;
         case EAST -> -90.0F;
         default -> this.stableYaw;
      };
   }

   @Generated
   public long getLastRightClickTime() {
      return this.lastRightClickTime;
   }

   @Generated
   public long getEnableTimeMs() {
      return this.enableTimeMs;
   }

   @Generated
   public float getStableYaw() {
      return this.stableYaw;
   }

   @Generated
   public int getOldSlot() {
      return this.oldSlot;
   }

   @Generated
   public int getLockedBlockSlot() {
      return this.lockedBlockSlot;
   }
}
