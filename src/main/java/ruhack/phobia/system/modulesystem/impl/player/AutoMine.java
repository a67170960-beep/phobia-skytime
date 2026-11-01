package ruhack.phobia.system.modulesystem.impl.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.utils.Rotation;
import com.adl.nativeprotect.Native;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.util.baritone.BaritoneHelper;
import ruhack.phobia.util.string.chat.ChatMessage;

public class AutoMine extends ModuleStructure {
   private static final int MIN_X = -86;
   private static final int MAX_X = -64;
   private static final int MIN_Y = 72;
   private static final int MAX_Y = 81;
   private static final int MIN_Z = -4;
   private static final int MAX_Z = 14;
   private static final int BORDER_TOLERANCE = 2;
   private static final int PLAYER_BOUNDARY_TOLERANCE = 1;
   private static final int WARP_SETTLE_TICKS = 40;
   private static final int SCAN_INTERVAL_TICKS = 10;
   private static final int NAVIGATE_INTERVAL_TICKS = 40;
   private static final int PICKUP_PATH_INTERVAL_TICKS = 10;
   private static final double BREAK_REACH_SQUARED = 22.0;
   private static final float BREAK_ANGLE_TOLERANCE = 7.0F;
   private static final Box RESOURCE_PICKUP_AREA = new Box(-88.0, 70.0, -6.0, -61.0, 84.0, 17.0);
   private static final AutoMine.MineAccess[] MINE_ACCESSES = new AutoMine.MineAccess[]{
      new AutoMine.MineAccess(new BlockPos(-88, 81, 6), new BlockPos(-86, 81, 6)),
      new AutoMine.MineAccess(new BlockPos(-77, 81, -7), new BlockPos(-77, 81, -4)),
      new AutoMine.MineAccess(new BlockPos(-64, 81, 5), new BlockPos(-64, 81, 5)),
      new AutoMine.MineAccess(new BlockPos(-76, 81, 16), new BlockPos(-76, 81, 14))
   };
   private static final Block[] ORES = new Block[]{
      Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.ANCIENT_DEBRIS, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE
   };
   private final BooleanSetting renderPath = new BooleanSetting("Показывать маршрут", "Отрисовывать путь Baritone до руды").setValue(true);
   private final BooleanSetting detectDrill = new BooleanSetting("Учитывать Бур", "Распознавать чар Бур и выбирать руду с учётом добычи 3x3").setValue(true);
   private BlockPos target;
   private int ticks;
   private int warpSentTick = -1073741824;
   private int lastScanTick = -1073741824;
   private int lastNavigateTick = -1073741824;
   private BlockPos pathTarget;
   private BlockPos breakingTarget;
   private int breakingStartedTick = -1073741824;
   private BlockPos mineReturnGoal;
   private AutoMine.MineAccess activeMineAccess;
   private BlockPos pickupPathTarget;
   private int pickupEntityId = -1;
   private int lastPickupPathTick = -1073741824;
   private boolean attackHeld;
   private boolean warpSent;
   private boolean navigatingToMine;
   private boolean enteringMine;
   private boolean inventoryBlockedByDrop;
   private boolean missingBaritoneReported;
   private final Set<BlockPos> blockedTargets = new HashSet<>();

   public AutoMine() {
      super("AutoMine", "Копает ценную руду на /warp mine через Baritone", ModuleCategory.PLAYER);
      this.settings(this.renderPath, this.detectDrill);
   }

   @Override
   public void activate() {
      this.resetRuntime();
   }

   @Override
   public void deactivate() {
      this.stopBreaking();
      this.stopMining();
      BaritoneHelper.applyMiningSettings(false, true);
      this.resetRuntime();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         this.ticks++;
         if (!BaritoneHelper.isAvailable()) {
            if (!this.missingBaritoneReported) {
               ChatMessage.brandmessage("AutoMine: Baritone не найден");
               this.missingBaritoneReported = true;
            }
         } else if (!this.warpSent && !this.isInsideMine(mc.player.getBlockPos(), 2)) {
            this.sendWarp();
         } else {
            BaritoneHelper.applyMiningSettings(true, this.renderPath.isValue());
            if (!this.warpSent || this.ticks - this.warpSentTick >= 40 || this.isInsideMine(mc.player.getBlockPos(), 1)) {
               boolean insideStrictMine = this.isInsideMine(mc.player.getBlockPos(), 0);
               boolean outsidePlayerMargin = !this.isInsideMine(mc.player.getBlockPos(), 1);
               boolean mustKeepReturning = this.navigatingToMine && !insideStrictMine;
               if (mustKeepReturning || !this.navigatingToMine && outsidePlayerMargin) {
                  if (!this.navigatingToMine && this.target != null) {
                     this.blockedTargets.add(this.target);
                  }

                  this.stopBreaking();
                  this.clearTargetForImmediateScan();
                  this.clearPickupTarget();
                  this.navigateToMine();
               } else {
                  BaritoneHelper.setAllowBreak(insideStrictMine);
                  if (this.navigatingToMine) {
                     BaritoneHelper.stop();
                     this.navigatingToMine = false;
                     this.mineReturnGoal = null;
                     this.activeMineAccess = null;
                     this.enteringMine = false;
                  }

                  if (this.target != null && !this.isWantedOre(mc.world.getBlockState(this.target).getBlock())) {
                     this.stopBreaking();
                     BaritoneHelper.stop();
                     this.clearTargetForImmediateScan();
                  }

                  ItemEntity resourceDrop = this.findNearestResourceDrop();
                  if (resourceDrop != null) {
                     this.handleResourcePickup(resourceDrop);
                  } else if (this.inventoryBlockedByDrop) {
                     this.stopBreaking();
                     BaritoneHelper.stop();
                     this.clearTargetForImmediateScan();
                     this.clearPickupTarget();
                  } else {
                     if (this.pickupEntityId != -1) {
                        BaritoneHelper.stop();
                        this.clearPickupTarget();
                     }

                     if (this.target == null && this.ticks - this.lastScanTick >= 10) {
                        this.lastScanTick = this.ticks;
                        this.target = this.findNearestOre();
                     }

                     if (this.target != null) {
                        if (this.canMineTarget()) {
                           this.stopTargetPath();
                           this.lookAtAndMineTarget();
                        } else {
                           this.stopBreaking();
                           this.pathToTarget();
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.stopBreaking();
      }
   }

   private void sendWarp() {
      if (mc.getNetworkHandler() != null) {
         this.stopMining();
         mc.getNetworkHandler().sendChatCommand("warp mine");
         this.warpSent = true;
         this.warpSentTick = this.ticks;
      }
   }

   @Native
   private void navigateToMine() {
      BaritoneHelper.setAllowBreak(false);
      if (!this.navigatingToMine) {
         this.navigatingToMine = true;
         this.activeMineAccess = this.nearestMineAccess(mc.player.getBlockPos());
         this.enteringMine = false;
         this.mineReturnGoal = null;
         BaritoneHelper.stop();
      }

      if (this.activeMineAccess == null) {
         this.activeMineAccess = this.nearestMineAccess(mc.player.getBlockPos());
      }

      if (!this.enteringMine && mc.player.getBlockPos().getSquaredDistance(this.activeMineAccess.ladder()) <= 6.25) {
         this.enteringMine = true;
         this.mineReturnGoal = null;
         BaritoneHelper.stop();
      }

      BlockPos goal = this.enteringMine ? this.activeMineAccess.entrance() : this.activeMineAccess.ladder();
      int range = this.enteringMine ? 0 : 1;
      if (!goal.equals(this.mineReturnGoal) || !BaritoneHelper.isPathing() && this.ticks - this.lastNavigateTick >= 40) {
         this.stopMining();
         BaritoneHelper.setAllowBreak(false);
         BaritoneHelper.goTo(goal, range);
         this.mineReturnGoal = goal;
         this.lastNavigateTick = this.ticks;
      }
   }

   private AutoMine.MineAccess nearestMineAccess(BlockPos playerPos) {
      AutoMine.MineAccess nearest = MINE_ACCESSES[0];
      double nearestDistance = playerPos.getSquaredDistance(nearest.ladder());

      for (int i = 1; i < MINE_ACCESSES.length; i++) {
         AutoMine.MineAccess candidate = MINE_ACCESSES[i];
         double distance = playerPos.getSquaredDistance(candidate.ladder());
         if (distance < nearestDistance) {
            nearestDistance = distance;
            nearest = candidate;
         }
      }

      return nearest;
   }

   @Native
   private void pathToTarget() {
      if (this.target != null) {
         if (!this.target.equals(this.pathTarget) || !BaritoneHelper.isPathing() && this.ticks - this.lastNavigateTick >= 40) {
            try {
               BaritoneHelper.stop();
               BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(this.target));
               this.pathTarget = this.target;
               this.lastNavigateTick = this.ticks;
            } catch (RuntimeException exception) {
               this.pathTarget = null;
            }
         }
      }
   }

   private boolean canMineTarget() {
      if (this.target == null) {
         return false;
      }

      Vec3d eye = mc.player.getEyePos();
      Vec3d center = Vec3d.ofCenter(this.target);
      if (eye.squaredDistanceTo(center) > 22.0) {
         return false;
      }

      BlockHitResult visibility = mc.world.raycast(new RaycastContext(eye, center, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
      return visibility.getType() == Type.BLOCK && visibility.getBlockPos().equals(this.target);
   }

   private void stopTargetPath() {
      if (this.pathTarget != null || BaritoneHelper.isPathing()) {
         BaritoneHelper.stop();
         this.pathTarget = null;
      }
   }

   private void lookAtAndMineTarget() {
      Vec3d eye = mc.player.getEyePos();
      Vec3d center = Vec3d.ofCenter(this.target);
      double dx = center.x - eye.x;
      double dy = center.y - eye.y;
      double dz = center.z - eye.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
      BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior().updateTarget(new Rotation(yaw, pitch), true);
      float yawError = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - yaw));
      float pitchError = Math.abs(mc.player.getPitch() - pitch);
      if (this.breakingTarget != null || !(yawError > 7.0F) && !(pitchError > 7.0F)) {
         if (mc.crosshairTarget instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(this.target)) {
            this.selectBestTool(mc.world.getBlockState(this.target));
            if (this.target.equals(this.breakingTarget)) {
               mc.options.attackKey.setPressed(true);
               this.attackHeld = true;
               if (this.ticks > this.breakingStartedTick) {
                  mc.interactionManager.updateBlockBreakingProgress(this.target, blockHit.getSide());
               }

               if ((this.ticks & 3) == 0) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            } else if (!mc.interactionManager.attackBlock(this.target, blockHit.getSide())) {
               this.stopBreaking();
            } else {
               this.breakingTarget = this.target;
               this.breakingStartedTick = this.ticks;
               mc.options.attackKey.setPressed(true);
               this.attackHeld = true;
               mc.player.swingHand(Hand.MAIN_HAND);
            }
         } else {
            this.stopBreaking();
         }
      } else {
         this.stopBreaking();
      }
   }

   private void stopBreaking() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.attackHeld && client != null && client.options != null) {
         client.options.attackKey.setPressed(false);
      }

      this.attackHeld = false;
      this.breakingTarget = null;
      this.breakingStartedTick = -1073741824;
   }

   @Native
   private void selectBestTool(BlockState state) {
      int bestSlot = mc.player.getInventory().getSelectedSlot();
      ItemStack bestStack = mc.player.getInventory().getStack(bestSlot);
      float bestSpeed = bestStack.getMiningSpeedMultiplier(state);
      boolean bestHasDrill = this.detectDrill.isValue() && this.hasDrillEnchant(bestStack);

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         float speed = stack.getMiningSpeedMultiplier(state);
         boolean hasDrill = this.detectDrill.isValue() && this.hasDrillEnchant(stack);
         if (speed > bestSpeed || Math.abs(speed - bestSpeed) < 0.001F && hasDrill && !bestHasDrill) {
            bestSpeed = speed;
            bestSlot = slot;
            bestHasDrill = hasDrill;
         }
      }

      if (mc.player.getInventory().getSelectedSlot() != bestSlot) {
         mc.player.getInventory().setSelectedSlot(bestSlot);
      }
   }

   private ItemEntity findNearestResourceDrop() {
      this.inventoryBlockedByDrop = false;
      if (this.pickupEntityId != -1) {
         if (mc.world.getEntityById(this.pickupEntityId) instanceof ItemEntity item
            && item.isAlive()
            && this.isWantedDrop(item.getStack())
            && RESOURCE_PICKUP_AREA.intersects(item.getBoundingBox())) {
            if (this.canInventoryAccept(item.getStack())) {
               return item;
            }

            this.inventoryBlockedByDrop = true;
         }

         this.clearPickupTarget();
      }

      ItemEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;

      for (ItemEntity item : mc.world
         .getEntitiesByClass(ItemEntity.class, RESOURCE_PICKUP_AREA, candidate -> candidate.isAlive() && this.isWantedDrop(candidate.getStack()))) {
         if (!this.canInventoryAccept(item.getStack())) {
            this.inventoryBlockedByDrop = true;
         } else {
            double distance = mc.player.squaredDistanceTo(item);
            if (distance < nearestDistance) {
               nearestDistance = distance;
               nearest = item;
            }
         }
      }

      return nearest;
   }

   private boolean canInventoryAccept(ItemStack incoming) {
      for (int slot = 0; slot < 36; slot++) {
         ItemStack existing = mc.player.getInventory().getStack(slot);
         if (existing.isEmpty()) {
            return true;
         }

         if (ItemStack.areItemsAndComponentsEqual(existing, incoming) && existing.getCount() < existing.getMaxCount()) {
            return true;
         }
      }

      return false;
   }

   private boolean isWantedDrop(ItemStack stack) {
      return stack.isOf(Items.DIAMOND)
         || stack.isOf(Items.RAW_GOLD)
         || stack.isOf(Items.GOLD_NUGGET)
         || stack.isOf(Items.GOLD_INGOT)
         || stack.isOf(Items.ANCIENT_DEBRIS)
         || stack.isOf(Items.DIAMOND_ORE)
         || stack.isOf(Items.DEEPSLATE_DIAMOND_ORE)
         || stack.isOf(Items.GOLD_ORE)
         || stack.isOf(Items.DEEPSLATE_GOLD_ORE)
         || stack.isOf(Items.NETHER_GOLD_ORE);
   }

   private void handleResourcePickup(ItemEntity item) {
      this.stopBreaking();
      this.target = null;
      this.pathTarget = null;
      BlockPos itemPos = item.getBlockPos();
      boolean changedDrop = this.pickupEntityId != item.getId();
      boolean moved = this.pickupPathTarget == null || !this.pickupPathTarget.equals(itemPos);
      if (changedDrop || moved || !BaritoneHelper.isPathing() && this.ticks - this.lastPickupPathTick >= 10) {
         BaritoneHelper.stop();
         if (BaritoneHelper.goTo(itemPos, 0)) {
            this.pickupEntityId = item.getId();
            this.pickupPathTarget = itemPos;
            this.lastPickupPathTick = this.ticks;
         }
      }
   }

   private void clearPickupTarget() {
      this.pickupEntityId = -1;
      this.pickupPathTarget = null;
      this.lastPickupPathTick = -1073741824;
   }

   private void clearTargetForImmediateScan() {
      this.target = null;
      this.pathTarget = null;
      this.lastScanTick = this.ticks - 10;
   }

   private void stopMining() {
      if (!BaritoneHelper.isAvailable()) {
         this.pathTarget = null;
      } else {
         BaritoneHelper.stop();
         this.pathTarget = null;
      }
   }

   private BlockPos findNearestOre() {
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos nearest = null;
      double bestScore = Double.MAX_VALUE;
      boolean drillActive = this.detectDrill.isValue() && this.hasUsableDrill();
      Mutable cursor = new Mutable();

      for (int x = -86; x <= -64; x++) {
         for (int y = 72; y <= 81; y++) {
            for (int z = -4; z <= 14; z++) {
               cursor.set(x, y, z);
               BlockState state = mc.world.getBlockState(cursor);
               if (this.isWantedOre(state.getBlock()) && !this.blockedTargets.contains(cursor)) {
                  double distance = playerPos.getSquaredDistance(cursor);
                  int drillYield = drillActive ? this.countDrillOres(cursor) : 1;
                  double score = distance - (drillYield - 1) * 7.0;
                  if (score < bestScore) {
                     bestScore = score;
                     nearest = cursor.toImmutable();
                  }
               }
            }
         }
      }

      return nearest;
   }

   private boolean hasUsableDrill() {
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && this.hasDrillEnchant(stack) && stack.getMiningSpeedMultiplier(Blocks.DIAMOND_ORE.getDefaultState()) > 1.0F) {
            return true;
         }
      }

      return false;
   }

   private boolean hasDrillEnchant(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }

      if (this.containsDrillName(stack.getName().getString())) {
         return true;
      }

      ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);

      for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
         if (this.containsDrillName(((Enchantment)enchantment.value()).description().getString())) {
            return true;
         }
      }

      LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
      if (lore != null) {
         for (Text line : lore.lines()) {
            if (this.containsDrillName(line.getString())) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean containsDrillName(String text) {
      String normalized = text.toLowerCase(Locale.ROOT);
      return normalized.contains("бур") || normalized.contains("drill");
   }

   private int countDrillOres(BlockPos center) {
      Axis normal = this.getDrillNormal(center);
      int count = 0;
      Mutable cursor = new Mutable();

      for (int first = -1; first <= 1; first++) {
         for (int second = -1; second <= 1; second++) {
            int x = center.getX();
            int y = center.getY();
            int z = center.getZ();
            if (normal == Axis.X) {
               y += first;
               z += second;
            } else if (normal == Axis.Y) {
               x += first;
               z += second;
            } else {
               x += first;
               y += second;
            }

            cursor.set(x, y, z);
            if (this.isInsideMine(cursor, 0) && !this.blockedTargets.contains(cursor) && this.isWantedOre(mc.world.getBlockState(cursor).getBlock())) {
               count++;
            }
         }
      }

      return count;
   }

   private Axis getDrillNormal(BlockPos block) {
      Vec3d delta = Vec3d.ofCenter(block).subtract(mc.player.getEyePos());
      double x = Math.abs(delta.x);
      double y = Math.abs(delta.y);
      double z = Math.abs(delta.z);
      if (y >= x && y >= z) {
         return Axis.Y;
      } else {
         return x >= z ? Axis.X : Axis.Z;
      }
   }

   private boolean isWantedOre(Block block) {
      for (Block ore : ORES) {
         if (block == ore) {
            return true;
         }
      }

      return false;
   }

   private boolean isInsideMine(BlockPos pos, int tolerance) {
      return pos.getX() >= -86 - tolerance
         && pos.getX() <= -64 + tolerance
         && pos.getY() >= 72 - tolerance
         && pos.getY() <= 81 + tolerance
         && pos.getZ() >= -4 - tolerance
         && pos.getZ() <= 14 + tolerance;
   }

   private void resetRuntime() {
      this.target = null;
      this.ticks = 0;
      this.warpSentTick = -1073741824;
      this.lastScanTick = -1073741824;
      this.lastNavigateTick = -1073741824;
      this.pathTarget = null;
      this.breakingTarget = null;
      this.breakingStartedTick = -1073741824;
      this.mineReturnGoal = null;
      this.activeMineAccess = null;
      this.clearPickupTarget();
      this.attackHeld = false;
      this.warpSent = false;
      this.navigatingToMine = false;
      this.enteringMine = false;
      this.inventoryBlockedByDrop = false;
      this.missingBaritoneReported = false;
      this.blockedTargets.clear();
   }

   private record MineAccess(BlockPos ladder, BlockPos entrance) {
   }
}
