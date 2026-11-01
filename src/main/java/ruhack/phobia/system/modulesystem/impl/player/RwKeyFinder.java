package ruhack.phobia.system.modulesystem.impl.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalNear;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.border.WorldBorder;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.ChatEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.timer.StopWatch;

public class RwKeyFinder extends ModuleStructure {
   private static final String LOOT_GAPPLE = "Чарки";
   private static final String LOOT_KEY = "Ключ-Карты";
   private static final int MIN_MINECART_Y = 10;
   private static final int MAX_MINECART_Y = 55;
   private static final int MAX_SEARCH_Y = 55;
   private static final int[] CAVE_SEARCH_LEVELS = new int[]{32, 24, 42, 16, 50};
   private static final int EXPLORE_DISTANCE = 384;
   private static final int BORDER_MARGIN = 64;
   private static final int EXPLORE_REPATH_MS = 3000;
   private static final double MINECART_SCAN_DISTANCE = 20.0;
   private static final int[][] EXPLORE_DIRECTIONS = new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
   private final MultiSelectSetting loot = new MultiSelectSetting("Что лутать?", "Предметы, которые нужно забирать")
      .value("Чарки", "Ключ-Карты")
      .selected("Чарки", "Ключ-Карты");
   private final BooleanSetting rememberLooted = new BooleanSetting("Запоминать облутанные", "Не возвращаться к уже проверенным вагонеткам").setValue(true);
   private final BooleanSetting autoLeave = new BooleanSetting("Auto leave (hub)", "Выходить в хаб при появлении игрока рядом");
   private final SliderSettings leaveRadius = new SliderSettings("Радиус обнаружения", "Дистанция срабатывания Auto leave", 16.0F)
      .range(1, 40)
      .visible(this.autoLeave::isValue);
   private final BooleanSetting autoInvest = new BooleanSetting("Auto invest", "Складывать найденные предметы в эндер-сундук");
   private final BooleanSetting autoHeal = new BooleanSetting("Auto heal", "Автоматически есть при низком здоровье");
   private final BooleanSetting autoFix = new BooleanSetting("Auto fix", "Использовать /fix all для повреждённой кирки");
   private final BooleanSetting dropTrash = new BooleanSetting("Выкидывать мусор", "Освобождать место, выбрасывая булыжник");
   private final StopWatch pathTimer = new StopWatch();
   private final StopWatch clickTimer = new StopWatch();
   private final StopWatch interactTimer = new StopWatch();
   private final StopWatch leaveTimer = new StopWatch();
   private final StopWatch fixTimer = new StopWatch();
   private final Set<String> rememberedMinecarts = new HashSet<>();
   private final Set<String> sessionLootedMinecarts = new HashSet<>();
   private final Map<String, Set<String>> foundLootByMinecart = new HashMap<>();
   private final Path keyfinderFile = Path.of("Phobia", "temp", "keyfinder.file");
   private ChestMinecartEntity targetMinecart;
   private String currentPathKey;
   private String openedMinecartKey;
   private boolean depositingToEnderChest;
   private boolean needsInvest;
   private boolean autoEating;
   private int exploreDirectionIndex;
   private int caveLevelIndex;
   private BlockPos exploreTarget;
   private BlockPos lastMinecartScanPos;
   private double nextMinecartScanDistance;

   public RwKeyFinder() {
      super("RwKeyFinder", "Ищет вагонетки с сундуком через Baritone", ModuleCategory.PLAYER);
      this.settings(this.loot, this.rememberLooted, this.autoLeave, this.leaveRadius, this.autoInvest, this.autoHeal, this.autoFix, this.dropTrash);
   }

   @Override
   public void activate() {
      this.loadRememberedMinecarts();
      this.resetRuntime();
   }

   @Override
   public void deactivate() {
      this.stopBaritone();
      this.stopUsingItem();
      this.resetRuntime();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null && mc.interactionManager != null) {
         if (!this.handleAutoLeave() && !this.handleAutoHeal()) {
            this.handleAutoFix();
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
               if (this.depositingToEnderChest) {
                  this.handleEnderChestDeposit(container);
               } else {
                  this.handleMinecartLoot(container);
               }
            } else if (this.autoInvest.isValue() && this.needsInvest && this.hasWantedLootInInventory()) {
               this.handleInvestPathing();
            } else {
               this.depositingToEnderChest = false;
               this.handleMinecartSearch();
            }
         }
      }
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      if (mc.player != null && mc.world != null && event.getMessage() != null) {
         String message = event.getMessage().toLowerCase(Locale.ROOT);
         if (this.isRegionDenyMessage(message)) {
            this.handleRegionDeny();
         }
      }
   }

   private boolean handleAutoLeave() {
      if (this.autoLeave.isValue() && this.leaveTimer.finished(5000.0)) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !FriendUtils.isFriend(player) && mc.player.distanceTo(player) <= this.leaveRadius.getValue()) {
               mc.getNetworkHandler().sendChatCommand("hub");
               this.stopBaritone();
               CompletableFuture.delayedExecutor(250L, TimeUnit.MILLISECONDS).execute(() -> mc.execute(this::stopBaritone));
               this.targetMinecart = null;
               this.currentPathKey = null;
               this.leaveTimer.reset();
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isRegionDenyMessage(String message) {
      return message.contains("не можете сломать блок здесь")
         || message.contains("не можете строить здесь")
         || message.contains("не можете") && message.contains("блок") && message.contains("здесь");
   }

   private void handleRegionDeny() {
      if (this.targetMinecart != null) {
         this.markBlockedMinecart(this.key(this.targetMinecart.getBlockPos()));
      } else if (this.openedMinecartKey != null) {
         this.markBlockedMinecart(this.openedMinecartKey);
      }

      this.stopBaritone();
      this.targetMinecart = null;
      this.openedMinecartKey = null;
      this.currentPathKey = null;
      this.exploreTarget = null;
      this.lastMinecartScanPos = null;

      for (int i = 0; i < EXPLORE_DIRECTIONS.length; i++) {
         this.rotateExploreDirection();
         BlockPos candidate = this.createExploreTarget();
         if (candidate != null) {
            this.exploreTarget = candidate;
            this.pathToExplore(candidate);
            return;
         }
      }
   }

   private boolean handleAutoHeal() {
      if (this.autoEating) {
         if (this.autoHeal.isValue() && !(mc.player.getHealth() > 15.0F) && mc.player.isUsingItem()) {
            return true;
         }

         this.stopUsingItem();
         return false;
      } else if (this.autoHeal.isValue() && !(mc.player.getHealth() > 15.0F) && !mc.player.isUsingItem() && mc.player.canConsume(false)) {
         int slot = this.findFoodSlot();
         if (slot == -1) {
            return false;
         }

         this.selectInventorySlot(slot);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         mc.options.useKey.setPressed(true);
         this.autoEating = true;
         return true;
      } else {
         return false;
      }
   }

   private void stopUsingItem() {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      this.autoEating = false;
   }

   private void handleAutoFix() {
      if (this.autoFix.isValue() && this.fixTimer.finished(30000.0)) {
         int slot = this.findDamagedPickaxeSlot();
         if (slot != -1) {
            this.selectInventorySlot(slot);
            mc.getNetworkHandler().sendChatCommand("fix all");
            this.fixTimer.reset();
         }
      }
   }

   private void selectInventorySlot(int inventorySlot) {
      int selected = mc.player.getInventory().getSelectedSlot();
      if (inventorySlot < 9) {
         mc.player.getInventory().setSelectedSlot(inventorySlot);
      } else {
         int handlerSlot = inventorySlot;
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, handlerSlot, selected, SlotActionType.SWAP, mc.player);
      }
   }

   private void handleMinecartSearch() {
      if (this.targetMinecart != null && !this.isValidMinecart(this.targetMinecart)) {
         this.targetMinecart = null;
         this.currentPathKey = null;
      }

      if (this.targetMinecart == null && this.shouldScanMinecarts()) {
         this.targetMinecart = this.findNearestMinecart();
         this.updateMinecartScanCheckpoint();
         if (this.targetMinecart != null) {
            this.currentPathKey = null;
            this.exploreTarget = null;
         }
      }

      if (this.targetMinecart == null) {
         this.handleDistantCaveSearch();
      } else {
         this.exploreTarget = null;
         if (mc.player.distanceTo(this.targetMinecart) <= 4.2) {
            if (this.interactTimer.finished(500.0)) {
               this.openedMinecartKey = this.key(this.targetMinecart.getBlockPos());
               mc.interactionManager.interactEntity(mc.player, this.targetMinecart, Hand.MAIN_HAND);
               this.interactTimer.reset();
            }
         } else {
            this.pathTo(this.targetMinecart.getBlockPos(), 2);
         }
      }
   }

   private boolean shouldScanMinecarts() {
      if (this.lastMinecartScanPos == null) {
         return true;
      }

      double dx = mc.player.getX() - this.lastMinecartScanPos.getX();
      double dz = mc.player.getZ() - this.lastMinecartScanPos.getZ();
      return dx * dx + dz * dz >= this.nextMinecartScanDistance * this.nextMinecartScanDistance;
   }

   private void updateMinecartScanCheckpoint() {
      this.lastMinecartScanPos = mc.player.getBlockPos();
      this.nextMinecartScanDistance = 20.0;
   }

   private void handleDistantCaveSearch() {
      if (mc.player.getY() > 55.0) {
         this.exploreTarget = new BlockPos(mc.player.getBlockX(), CAVE_SEARCH_LEVELS[this.caveLevelIndex], mc.player.getBlockZ());
         this.currentPathKey = null;
         this.pathToExplore(this.exploreTarget);
      } else {
         if (this.shouldTurnAwayFromBorder()) {
            this.rotateExploreDirection();
            this.exploreTarget = null;
            this.currentPathKey = null;
         }

         boolean reached = this.isNearExploreTarget();
         if (this.exploreTarget == null || reached) {
            if (reached) {
               this.caveLevelIndex = (this.caveLevelIndex + 1) % CAVE_SEARCH_LEVELS.length;
            }

            this.exploreTarget = this.createExploreTarget();
            this.currentPathKey = null;
         }

         if (this.exploreTarget == null) {
            this.rotateExploreDirection();
            this.exploreTarget = this.createExploreTarget();
         }

         if (this.exploreTarget != null) {
            this.pathToExplore(this.exploreTarget);
         }
      }
   }

   private void handleMinecartLoot(GenericContainerScreenHandler container) {
      if (this.clickTimer.finished(120.0)) {
         int containerSlots = container.getRows() * 9;
         int wantedSlots = this.countWantedLootSlots(container, containerSlots);
         if (wantedSlots > 0 && this.dropTrash.isValue()) {
            int missing = wantedSlots - this.countEmptyInventorySlots();
            if (missing > 0 && this.dropCobblestoneStacks(container, containerSlots, missing)) {
               this.clickTimer.reset();
               return;
            }
         }

         for (int i = 0; i < containerSlots; i++) {
            Slot slot = container.getSlot(i);
            if (slot.hasStack() && this.isWantedLoot(slot.getStack())) {
               this.rememberFoundLoot(this.openedMinecartKey, slot.getStack());
               mc.interactionManager.clickSlot(container.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
               this.needsInvest = true;
               this.clickTimer.reset();
               return;
            }
         }

         this.markMinecartLooted(this.openedMinecartKey);
         mc.player.closeHandledScreen();
         this.openedMinecartKey = null;
         this.targetMinecart = null;
         this.currentPathKey = null;
         this.clickTimer.reset();
      }
   }

   private void handleInvestPathing() {
      BlockPos enderChest = this.findNearestEnderChest(16);
      if (enderChest != null) {
         if (mc.player.squaredDistanceTo(Vec3d.ofCenter(enderChest)) <= 20.25) {
            if (this.interactTimer.finished(500.0)) {
               this.depositingToEnderChest = true;
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(enderChest), Direction.UP, enderChest, false));
               this.interactTimer.reset();
            }
         } else {
            this.pathTo(enderChest, 2);
         }
      }
   }

   private void handleEnderChestDeposit(GenericContainerScreenHandler container) {
      if (this.clickTimer.finished(120.0)) {
         int containerSlots = container.getRows() * 9;

         for (int i = containerSlots; i < container.slots.size(); i++) {
            Slot slot = container.getSlot(i);
            if (slot.hasStack() && this.isWantedLoot(slot.getStack())) {
               mc.interactionManager.clickSlot(container.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
               this.clickTimer.reset();
               return;
            }
         }

         this.needsInvest = this.hasWantedLootInInventory();
         this.depositingToEnderChest = false;
         mc.player.closeHandledScreen();
         this.clickTimer.reset();
      }
   }

   private void pathTo(BlockPos pos, int range) {
      String pathKey = this.key(pos);
      if (!pathKey.equals(this.currentPathKey) || this.pathTimer.finished(1500.0)) {
         BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(pos, range));
         this.currentPathKey = pathKey;
         this.pathTimer.reset();
      }
   }

   private void pathToExplore(BlockPos pos) {
      String pathKey = "explore:" + this.key(pos);
      if (!pathKey.equals(this.currentPathKey) || this.pathTimer.finished(3000.0)) {
         BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(pos, 2));
         this.currentPathKey = pathKey;
         this.pathTimer.reset();
      }
   }

   private BlockPos createExploreTarget() {
      int[] direction = EXPLORE_DIRECTIONS[this.exploreDirectionIndex];
      WorldBorder border = mc.world.getWorldBorder();
      int currentX = mc.player.getBlockX();
      int currentZ = mc.player.getBlockZ();
      int targetX = currentX + direction[0] * 384;
      int targetZ = currentZ + direction[1] * 384;
      int targetY = Math.min(CAVE_SEARCH_LEVELS[this.caveLevelIndex], 50);
      int minX = (int)Math.ceil(border.getBoundWest()) + 64;
      int maxX = (int)Math.floor(border.getBoundEast()) - 64;
      int minZ = (int)Math.ceil(border.getBoundNorth()) + 64;
      int maxZ = (int)Math.floor(border.getBoundSouth()) - 64;
      targetX = Math.max(minX, Math.min(maxX, targetX));
      targetZ = Math.max(minZ, Math.min(maxZ, targetZ));
      return (direction[0] <= 0 || currentX < maxX - 64)
            && (direction[0] >= 0 || currentX > minX + 64)
            && (direction[1] <= 0 || currentZ < maxZ - 64)
            && (direction[1] >= 0 || currentZ > minZ + 64)
         ? new BlockPos(targetX, targetY, targetZ)
         : null;
   }

   private boolean isNearExploreTarget() {
      if (this.exploreTarget == null) {
         return true;
      }

      double dx = mc.player.getX() - this.exploreTarget.getX();
      double dy = mc.player.getY() - this.exploreTarget.getY();
      double dz = mc.player.getZ() - this.exploreTarget.getZ();
      return dx * dx + dz * dz <= 2304.0 && Math.abs(dy) <= 18.0;
   }

   private boolean shouldTurnAwayFromBorder() {
      WorldBorder border = mc.world.getWorldBorder();
      int[] direction = EXPLORE_DIRECTIONS[this.exploreDirectionIndex];
      double x = mc.player.getX();
      double z = mc.player.getZ();
      return direction[0] > 0 && border.getBoundEast() - x <= 128.0
         || direction[0] < 0 && x - border.getBoundWest() <= 128.0
         || direction[1] > 0 && border.getBoundSouth() - z <= 128.0
         || direction[1] < 0 && z - border.getBoundNorth() <= 128.0;
   }

   private void rotateExploreDirection() {
      this.exploreDirectionIndex = (this.exploreDirectionIndex + 1) % EXPLORE_DIRECTIONS.length;
      this.caveLevelIndex = (this.caveLevelIndex + 1) % CAVE_SEARCH_LEVELS.length;
   }

   private ChestMinecartEntity findNearestMinecart() {
      ChestMinecartEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof ChestMinecartEntity minecart && this.isValidMinecart(minecart)) {
            double distance = mc.player.squaredDistanceTo(minecart);
            if (distance < nearestDistance) {
               nearestDistance = distance;
               nearest = minecart;
            }
         }
      }

      return nearest;
   }

   private boolean isValidMinecart(ChestMinecartEntity minecart) {
      if (minecart != null && !minecart.isRemoved()) {
         BlockPos pos = minecart.getBlockPos();
         if (mc.world.getWorldBorder().contains(pos) && pos.getY() >= 10 && pos.getY() <= 55) {
            String key = this.key(pos);
            return !this.sessionLootedMinecarts.contains(key) && (!this.rememberLooted.isValue() || !this.isRememberedMinecart(key));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private BlockPos findNearestEnderChest(int radius) {
      BlockPos origin = mc.player.getBlockPos();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -4; y <= 4; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos pos = origin.add(x, y, z);
               if (mc.world.getBlockState(pos).isOf(Blocks.ENDER_CHEST)) {
                  double distance = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                  if (distance < bestDistance) {
                     bestDistance = distance;
                     best = pos;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean isWantedLoot(ItemStack stack) {
      return stack.isEmpty()
         ? false
         : this.loot.isSelected("Чарки") && stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || this.loot.isSelected("Ключ-Карты") && this.isMapKey(stack);
   }

   private boolean isMapKey(ItemStack stack) {
      return stack.isOf(Items.NAME_TAG)
         && Registries.ITEM.getId(stack.getItem()).toString().equals("minecraft:name_tag")
         && stack.getName().getString().toLowerCase(Locale.ROOT).contains("ключ");
   }

   private boolean hasWantedLootInInventory() {
      for (ItemStack stack : mc.player.getInventory().getMainStacks()) {
         if (this.isWantedLoot(stack)) {
            return true;
         }
      }

      return false;
   }

   private int findFoodSlot() {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && this.isHealFood(stack.getItem())) {
            return i;
         }
      }

      return -1;
   }

   private int findDamagedPickaxeSlot() {
      int bestSlot = -1;
      int bestDamage = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.isIn(ItemTags.PICKAXES) && stack.isDamageable()) {
            int damage = stack.getDamage();
            if (damage > 0 && damage * 2 >= stack.getMaxDamage() && damage > bestDamage) {
               bestDamage = damage;
               bestSlot = i;
            }
         }
      }

      return bestSlot;
   }

   private boolean isHealFood(Item item) {
      return item == Items.GOLDEN_CARROT
         || item == Items.COOKED_BEEF
         || item == Items.BEEF
         || item == Items.COOKED_PORKCHOP
         || item == Items.PORKCHOP
         || item == Items.COOKED_CHICKEN
         || item == Items.CHICKEN
         || item == Items.COOKED_MUTTON
         || item == Items.MUTTON
         || item == Items.COOKED_RABBIT
         || item == Items.RABBIT
         || item == Items.ROTTEN_FLESH;
   }

   private int countWantedLootSlots(ScreenHandler container, int containerSlotCount) {
      int count = 0;

      for (int i = 0; i < containerSlotCount; i++) {
         Slot slot = container.getSlot(i);
         if (slot.hasStack() && this.isWantedLoot(slot.getStack())) {
            count++;
         }
      }

      return count;
   }

   private int countEmptyInventorySlots() {
      int count = 0;

      for (ItemStack stack : mc.player.getInventory().getMainStacks()) {
         if (stack.isEmpty()) {
            count++;
         }
      }

      return count;
   }

   private boolean dropCobblestoneStacks(ScreenHandler container, int containerSlots, int stacksToDrop) {
      int dropped = 0;

      for (int i = containerSlots; i < container.slots.size() && dropped < stacksToDrop; i++) {
         Slot slot = container.getSlot(i);
         if (slot.hasStack() && slot.getStack().isOf(Items.COBBLESTONE)) {
            mc.interactionManager.clickSlot(container.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
            dropped++;
         }
      }

      return dropped > 0;
   }

   private void rememberFoundLoot(String key, ItemStack stack) {
      if (key != null && !key.isBlank() && !stack.isEmpty()) {
         this.foundLootByMinecart.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(stack.getName().getString());
      }
   }

   private void markMinecartLooted(String key) {
      if (key != null && !key.isBlank()) {
         this.sessionLootedMinecarts.add(key);
         if (this.rememberLooted.isValue()) {
            String entry = this.getRememberedEntry(key);
            boolean changed = this.rememberedMinecarts.removeIf(saved -> this.isRememberedEntryForKey(saved, key));
            if (this.rememberedMinecarts.add(entry) || changed) {
               this.saveRememberedMinecarts();
            }
         }

         this.foundLootByMinecart.remove(key);
      }
   }

   private void markBlockedMinecart(String key) {
      if (key != null && !key.isBlank()) {
         this.sessionLootedMinecarts.add(key);
         this.foundLootByMinecart.remove(key);
         boolean changed = this.rememberedMinecarts.removeIf(saved -> this.isRememberedEntryForKey(saved, key));
         if (this.rememberedMinecarts.add(key) || changed) {
            this.saveRememberedMinecarts();
         }
      }
   }

   private String getRememberedEntry(String key) {
      Set<String> foundLoot = this.foundLootByMinecart.get(key);
      return foundLoot != null && !foundLoot.isEmpty() ? key + " - найден предмет: " + String.join(", ", foundLoot) : key;
   }

   private boolean isRememberedMinecart(String key) {
      return this.rememberedMinecarts.stream().anyMatch(entry -> this.isRememberedEntryForKey(entry, key));
   }

   private boolean isRememberedEntryForKey(String entry, String key) {
      return entry.equals(key) || entry.startsWith(key + " - ");
   }

   private void loadRememberedMinecarts() {
      this.rememberedMinecarts.clear();

      try {
         if (Files.notExists(this.keyfinderFile)) {
            return;
         }

         for (String line : Files.readAllLines(this.keyfinderFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
               this.rememberedMinecarts.add(trimmed);
            }
         }
      } catch (Exception var4) {
      }
   }

   private void saveRememberedMinecarts() {
      try {
         Files.createDirectories(this.keyfinderFile.getParent());
         Files.write(this.keyfinderFile, this.rememberedMinecarts.stream().sorted().toList(), StandardCharsets.UTF_8);
      } catch (Exception var2) {
      }
   }

   private void stopBaritone() {
      try {
         BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
      } catch (Exception var2) {
      }
   }

   private void resetRuntime() {
      this.targetMinecart = null;
      this.currentPathKey = null;
      this.openedMinecartKey = null;
      this.depositingToEnderChest = false;
      this.needsInvest = false;
      this.autoEating = false;
      this.foundLootByMinecart.clear();
      this.exploreDirectionIndex = Math.floorMod(mc.player == null ? 0 : Math.round(mc.player.getYaw() / 90.0F), EXPLORE_DIRECTIONS.length);
      this.caveLevelIndex = 0;
      this.exploreTarget = null;
      this.lastMinecartScanPos = null;
      this.nextMinecartScanDistance = 20.0;
      this.pathTimer.reset();
      this.clickTimer.reset();
      this.interactTimer.reset();
      this.leaveTimer.reset();
      this.fixTimer.reset();
   }

   private String key(BlockPos pos) {
      return pos.getX() + " " + pos.getY() + " " + pos.getZ();
   }
}
