package ruhack.phobia.system.modulesystem.impl.movement;

import java.util.stream.Stream;
import lombok.Generated;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.RotationUpdateEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.impl.rage.killaura.rotation.angle.basic.LinearSmoothMode;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.util.math.TaskPriority;
import ruhack.phobia.util.player.SimulatedPlayer;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.rotation.RotationConfig;
import ruhack.phobia.util.rotation.RotationController;
import ruhack.phobia.util.rotation.angle.Angle;
import ruhack.phobia.util.rotation.mod.AngleUtil;
import ruhack.phobia.util.string.PlayerInteractionHelper;
import ruhack.phobia.util.timer.StopWatch;

public class Spider extends ModuleStructure {
   private final StopWatch stopWatch = new StopWatch();
   private final SelectSetting mode = new SelectSetting("Режим", "Выбирает режим", "SpookyTime", "SpookyTime", "FunTime", "Slime Block", "Water Bucket");
   private int cooldown;
   private boolean startSetPitch = false;

   public Spider() {
      super("Spider", "Позволяет игрока лазить по стенам", ModuleCategory.MOVEMENT);
      this.settings(this.mode);
   }

   private Block getBlockState(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getBlock();
   }

   @Override
   public void deactivate() {
      if (this.mode.isSelected("Slime Block")) {
         mc.options.jumpKey.setPressed(false);
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         if (this.mode.isSelected("FunTime")) {
            this.handleFunTimeMode();
         }

         if (this.mode.isSelected("Water Bucket")) {
            this.handleWaterBucketMode();
         }

         if (this.mode.isSelected("SpookyTime") && this.stopWatch.finished(310.0)) {
            this.handleSpookyTimeMode();
         }

         if (this.mode.isSelected("Slime Block")) {
            this.handleSlimeBlock();
         }
      }
   }

   private void handleFunTimeMode() {
      if (!mc.options.jumpKey.isPressed()) {
         Box playerBox = mc.player.getBoundingBox().expand(-0.001);
         Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
         if (this.stopWatch.finished(400.0) && PlayerInteractionHelper.isBox(box, this::hasCollision)) {
            box = new Box(playerBox.minX - 0.3, playerBox.minY + 1.0, playerBox.minZ - 0.3, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
            if (PlayerInteractionHelper.isBox(box, this::hasCollision)) {
               mc.player.setOnGround(true);
               mc.player.setVelocity(mc.player.getVelocity().x, 0.6, mc.player.getVelocity().z);
            } else {
               mc.player.setOnGround(true);
               mc.player.jump();
            }
         }
      }
   }

   private void handleWaterBucketMode() {
      if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.player.swingHand(Hand.MAIN_HAND);
         mc.player.setVelocity(mc.player.getVelocity().x, 0.3, mc.player.getVelocity().z);
      }
   }

   private void handleSpookyTimeMode() {
      if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
         mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
         mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
      }

      this.stopWatch.reset();
   }

   private void handleSlimeBlock() {
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos[] adjacentBlocks = new BlockPos[]{playerPos.east(), playerPos.west(), playerPos.north(), playerPos.south()};
      boolean hasAdjacentSlime = false;

      for (BlockPos pos : adjacentBlocks) {
         if (this.getBlockState(pos) == Blocks.SLIME_BLOCK) {
            hasAdjacentSlime = true;
            break;
         }
      }

      if (hasAdjacentSlime && mc.player.horizontalCollision && !(mc.player.getVelocity().y <= -1.0)) {
         if (mc.crosshairTarget instanceof BlockHitResult blockHit) {
            Direction face = blockHit.getSide();
            BlockPos targetPos = blockHit.getBlockPos();
            if (this.getBlockState(targetPos) == Blocks.AIR) {
               return;
            }

            int slimeSlot = this.findHotbarSlot(Items.SLIME_BLOCK);
            if (slimeSlot != -1) {
               InventoryUtils.selectSlot(slimeSlot);
               this.startSetPitch = true;
               mc.player.setPitch(54.0F);
               BlockHitResult interaction = new BlockHitResult(blockHit.getPos(), face, targetPos, false);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, interaction);
               mc.player.swingHand(Hand.MAIN_HAND);
               if (this.cooldown >= 0.5) {
                  mc.player.setVelocity(mc.player.getVelocity().x, 0.63, mc.player.getVelocity().z);
                  this.cooldown = 0;
               } else {
                  this.cooldown++;
               }
            }
         }
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 0) {
         if (mc.player != null && mc.world != null) {
            if (this.mode.isSelected("Slime Block")) {
               boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
               int slotId = this.findHotbarBlockSlot();
               BlockPos blockPos = this.findPos();
               if ((offHand || slotId != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
                  ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
                  Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                  Vec3d vec = blockPos.toCenterPos();
                  Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
                  Angle angle = AngleUtil.calculateAngle(vec.subtract(new Vec3d(direction.getVector()).multiply(0.1F)));
                  Angle.VecRotation vecRotation = new Angle.VecRotation(angle, angle.toVector());
                  RotationController.INSTANCE
                     .rotateTo(vecRotation, mc.player, 1, new RotationConfig(new LinearSmoothMode(), true, true, false), TaskPriority.HIGH_IMPORTANCE_1, this);
                  if (this.canPlace(stack)) {
                     int prev = mc.player.getInventory().getSelectedSlot();
                     if (!offHand) {
                        InventoryUtils.selectSlot(slotId);
                     }

                     mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
                     mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                     if (!offHand) {
                        InventoryUtils.selectSlot(prev);
                     }
                  }
               }
            }
         }
      }
   }

   private int findHotbarSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int findHotbarBlockSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
            return i;
         }
      }

      return -1;
   }

   private boolean canPlace(ItemStack stack) {
      BlockPos blockPos = this.getBlockPos();
      if (blockPos.getY() >= mc.player.getBlockY()) {
         return false;
      }

      BlockItem blockItem = (BlockItem)stack.getItem();
      VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
      if (shape.isEmpty()) {
         return false;
      }

      Box box = shape.getBoundingBox().offset(blockPos);
      return !box.intersects(mc.player.getBoundingBox()) && box.intersects(SimulatedPlayer.simulateLocalPlayer(4).boundingBox);
   }

   private BlockPos findPos() {
      BlockPos blockPos = this.getBlockPos();
      return mc.world.getBlockState(blockPos).isSolid()
         ? BlockPos.ORIGIN
         : Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north())
            .filter(pos -> mc.world.getBlockState(pos).isSolid())
            .findFirst()
            .orElse(BlockPos.ORIGIN);
   }

   private BlockPos getBlockPos() {
      return BlockPos.ofFloored(SimulatedPlayer.simulateLocalPlayer(1).pos.add(0.0, -0.001, 0.0));
   }

   private boolean hasCollision(BlockPos blockPos) {
      return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
   }

   @Generated
   public void setCooldown(int cooldown) {
      this.cooldown = cooldown;
   }

   @Generated
   public void setStartSetPitch(boolean startSetPitch) {
      this.startSetPitch = startSetPitch;
   }

   @Generated
   public StopWatch getStopWatch() {
      return this.stopWatch;
   }

   @Generated
   public SelectSetting getMode() {
      return this.mode;
   }

   @Generated
   public int getCooldown() {
      return this.cooldown;
   }

   @Generated
   public boolean isStartSetPitch() {
      return this.startSetPitch;
   }
}
