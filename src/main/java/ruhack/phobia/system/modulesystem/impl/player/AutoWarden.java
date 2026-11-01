package ruhack.phobia.system.modulesystem.impl.player;

import com.adl.nativeprotect.Native;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.eventsystem.impl.WorldRenderEvent;
import ruhack.phobia.system.friendsystem.FriendUtils;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.system.modulesystem.setting.implement.TextSetting;
import ruhack.phobia.system.rendersystem.render3d.fill.Fill3D;
import ruhack.phobia.system.rendersystem.render3d.line.Line3D;
import ruhack.phobia.util.baritone.BaritoneHelper;
import ruhack.phobia.util.istance.Instance;
import ruhack.phobia.util.string.PlayerInteractionHelper;

public class AutoWarden extends ModuleStructure {
   private static final int WARDEN_CHEST_Y_TOLERANCE = 1;
   private static final int EXTRA_CHEST_HOLOGRAM_Y_SCAN = 5;
   private static final int INVIS_SKIP_POTION_SECONDS = 180;
   private static final int MIN_DARENA_LEAVE_SECONDS = 12;
   private static final int MARKED_CHEST_APPROACH_SECONDS = 60;
   private static final double JOIN_HOLOGRAM_REFRESH_DISTANCE = 8.0;
   private static final List<BlockPos> WARDEN_CHESTS = List.of(
      new BlockPos(1982, -49, 1960),
      new BlockPos(1988, -56, 1946),
      new BlockPos(1940, -56, 1960),
      new BlockPos(1948, -56, 1957),
      new BlockPos(1941, -50, 1960),
      new BlockPos(1957, -56, 2014),
      new BlockPos(2016, -55, 1946),
      new BlockPos(1958, -55, 1987),
      new BlockPos(1988, -54, 1975),
      new BlockPos(1960, -55, 1993),
      new BlockPos(1985, -55, 2039),
      new BlockPos(2049, -55, 1957),
      new BlockPos(2033, -56, 1972),
      new BlockPos(1942, -55, 2041),
      new BlockPos(1948, -49, 2041),
      new BlockPos(2055, -49, 1968),
      new BlockPos(1968, -56, 2050)
   );
   private static final Set<BlockEntityType<?>> CHEST_TYPES = Set.of(BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST);
   private static final Set<Item> VALUABLE_LOOT_ITEMS = Set.of(
      Items.NETHERITE_SCRAP,
      Items.NETHERITE_HELMET,
      Items.NETHERITE_CHESTPLATE,
      Items.NETHERITE_LEGGINGS,
      Items.NETHERITE_BOOTS,
      Items.TRIPWIRE_HOOK,
      Items.ENDER_EYE,
      Items.PAPER,
      Items.SPLASH_POTION,
      Items.SUGAR,
      Items.IRON_NUGGET,
      Items.ELYTRA,
      Items.NETHERITE_SWORD,
      Items.POPPED_CHORUS_FRUIT,
      Items.EXPERIENCE_BOTTLE,
      Items.TOTEM_OF_UNDYING,
      Items.GOLDEN_APPLE,
      Items.PLAYER_HEAD,
      Items.PHANTOM_MEMBRANE,
      Items.TNT,
      Items.EMERALD_ORE,
      Items.BEACON,
      Items.NETHERITE_PICKAXE,
      Items.DRIED_KELP,
      Items.AMETHYST_SHARD,
      Items.NETHER_STAR,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.FEATHER,
      Items.GUNPOWDER,
      Items.VILLAGER_SPAWN_EGG,
      Items.REDSTONE_TORCH
   );
   private static final Pattern TIMER_MMSS = Pattern.compile("(\\d{1,2})\\s*[:.]\\s*(\\d{1,2})");
   private static final Pattern TIMER_SECOND_UNIT = Pattern.compile("(\\d{1,3})\\s*(?:сек(?:унд(?:ы|у|а)?)?|s|sec|seconds?)", 66);
   private static final Pattern TIMER_MINUTE_UNIT = Pattern.compile("(\\d{1,2})\\s*(?:мин(?:ут(?:ы|у|а)?)?|m|min|minutes?)", 66);
   private static final Pattern TIMER_NUMBER = Pattern.compile("(\\d{1,3})");
   private static final int DARENA_SLOT = 24;
   private static final long COMMAND_DELAY_MS = 850L;
   private static final long PATH_REISSUE_MS = 1200L;
   private static final long DARENA_GUI_TIMEOUT_MS = 4000L;
   private static final long OPEN_RETRY_MS = 700L;
   private static final long INSPECT_COOLDOWN_MS = 30000L;
   private static final long CHEST_INTERACTION_LOCK_MS = 0L;
   private static final long STORAGE_JOIN_JUMP_MS = 250L;
   private static final long DRINK_CLOSE_SCREEN_DELAY_MS = 120L;
   private static final long DRINK_SWAP_DELAY_MS = 180L;
   private static final long DRINK_SELECT_DELAY_MS = 80L;
   private static final long DRINK_MIN_USE_MS = 1200L;
   private static final long DRINK_TIMEOUT_MS = 5000L;
   private static final long DRINK_RESTORE_DELAY_MS = 120L;
   private final TextSetting storageAnarchy = new TextSetting("Анархия склада", "Номер анархии со складом", "308");
   private final TextSetting wardenAnarchy = new TextSetting("Анархия вардена", "Номер анархии с городом вардена", "110");
   private final TextSetting homeName = new TextSetting("Название home", "Точка дома для телепорта", "home");
   private final SliderSettings carrotsToTake = new SliderSettings("Моркови брать", "Сколько золотой моркови брать со склада", 3.0F).range(1, 16);
   private final SliderSettings potionsToTake = new SliderSettings("Зелий брать", "Сколько зелий невидимости брать со склада", 1.0F).range(1, 4);
   private final SliderSettings valuableItemsPerTrip = new SliderSettings("Ценных за заход", "Количество ценных предметов за один заход", 2.0F).range(1, 16);
   private final SliderSettings lootTimerThreshold = new SliderSettings("Заходить при сек", "Порог таймера сундука для подготовки лута", 50.0F).range(15, 90);
   private final SliderSettings homeWaitSeconds = new SliderSettings("Ожидание home", "Сколько секунд ждать телепорт /home", 8.0F).range(5, 12);
   private final SliderSettings homeLeadSeconds = new SliderSettings("Запас до открытия", "Запас секунд до открытия сундука", 2.0F).range(1, 5);
   private final SliderSettings anarchyWaitSeconds = new SliderSettings("Ожидание /an", "Сколько секунд ждать переход анархии", 4.0F)
      .range(2.0F, 10.0F)
      .step(0.5F);
   private final SliderSettings stashScanRadius = new SliderSettings("Радиус склада", "Радиус поиска сундуков склада", 8.0F).range(4, 16);
   private final SliderSettings playerDangerRadius = new SliderSettings("Радиус игрока", "Опасный радиус до чужого игрока", 13.0F).range(5, 32);
   private final SliderSettings wardenDangerRadius = new SliderSettings("Радиус вардена", "Опасный радиус до вардена", 20.0F).range(8, 40);
   private final BooleanSetting ignoreFriends = new BooleanSetting("Игнор друзей", "Не считать друзей опасностью").setValue(true);
   private final BooleanSetting autoRespawn = new BooleanSetting("Авто-респавн", "Автоматически возрождаться после смерти").setValue(true);
   private final BooleanSetting showHud = new BooleanSetting("Показывать HUD", "Панель статуса бота").setValue(true);
   private final BooleanSetting showRoute = new BooleanSetting("Показывать маршрут", "Линия и подсветка целевого сундука").setValue(true);
   private final Map<BlockPos, AutoWarden.ChestTimerInfo> chestTimers = new HashMap<>();
   private final Map<BlockPos, Long> inspectedCooldowns = new HashMap<>();
   private final Set<BlockPos> discoveredExtraWardenChests = new LinkedHashSet<>();
   private final Set<BlockPos> patrolVisitedChests = new LinkedHashSet<>();
   private AutoWarden.Phase phase = AutoWarden.Phase.IDLE;
   private long startedAt;
   private long phaseStartedAt;
   private long lastCommandAt;
   private long nextPathAt;
   private long nextOpenAt;
   private long nextRepathAt;
   private long wardenEnteredAt;
   private long targetOpenTime;
   private long storageJoinJumpStartedAt;
   private String action = "Ожидание";
   private BlockPos storageChest;
   private BlockPos supplyChest;
   private BlockPos currentGoal;
   private BlockPos currentChest;
   private BlockPos lastLootedChest;
   private BlockPos detourGoal;
   private BlockPos hologramRefreshGoal;
   private boolean pvpMode;
   private boolean wasDead;
   private boolean detouring;
   private boolean lastRouteRenderSetting;
   private boolean drinkingInvis;
   private boolean preparedWardenReturn;
   private boolean storageJoinJumpDone;
   private AutoWarden.DrinkStage drinkStage = AutoWarden.DrinkStage.IDLE;
   private long drinkStageStartedAt;
   private long drinkStartedAt;
   private boolean potionWasInventory;
   private int potionSourceSlot = -1;
   private int potionHotbarSlot = -1;
   private int restoreHotbarSlot = -1;
   private int cycles;
   private int lootedChests;
   private int valuableItemsThisTrip;
   private int collectedStacks;
   private int depositedStacks;
   private int depositSessionStart;
   private int deaths;

   public static AutoWarden getInstance() {
      return Instance.get(AutoWarden.class);
   }

   public AutoWarden() {
      super("AutoWarden", "Авто-лутер города вардена через Baritone", ModuleCategory.PLAYER);
      this.settings(
         this.storageAnarchy,
         this.wardenAnarchy,
         this.homeName,
         this.carrotsToTake,
         this.potionsToTake,
         this.valuableItemsPerTrip,
         this.lootTimerThreshold,
         this.homeWaitSeconds,
         this.homeLeadSeconds,
         this.anarchyWaitSeconds,
         this.stashScanRadius,
         this.playerDangerRadius,
         this.wardenDangerRadius,
         this.ignoreFriends,
         this.autoRespawn,
         this.showHud,
         this.showRoute
      );
   }

   @Override
   public void activate() {
      this.resetRuntime();
      this.applyAutoWardenBaritoneSettings();
      this.setPhase(AutoWarden.Phase.GO_STORAGE_ANARCHY, "Перехожу на анархию склада");
   }

   @Override
   public void deactivate() {
      this.finishDrinkingIfNeeded();
      this.releaseUseKey();
      this.releaseStorageJoinJump();
      BaritoneHelper.stop();
      BaritoneHelper.applySettings(false, true);
   }

   public boolean showHudPanel() {
      return this.showHud.isValue();
   }

   public boolean isBlockedPhase() {
      return this.phase == AutoWarden.Phase.BLOCKED;
   }

   public List<String> hudLines() {
      List<String> lines = new ArrayList<>();
      lines.add("Текущая фаза: " + this.phase.label);
      lines.add("Прошло времени: " + this.formatDuration((System.currentTimeMillis() - this.startedAt) / 1000L));
      lines.add("Циклов: " + this.cycles);
      lines.add("Сундуков слутано: " + this.lootedChests);
      lines.add("Ценных за заход: " + this.valuableItemsThisTrip + "/" + this.valuableItemsPerTrip.getInt());
      lines.add("Ценностей собрано: " + this.collectedStacks);
      lines.add("Выгружено в хранилище: " + this.depositedStacks);
      lines.add("Смертей: " + this.deaths);
      lines.add("Действие: " + this.action);
      return lines;
   }

   @EventHandler
   @Native
   public void onTick(TickEvent event) {
      if (!PlayerInteractionHelper.nullCheck()) {
         this.scanWardenChestTimers();
         this.updatePvpMode();
         this.syncRouteRenderSetting();
         if (!this.handleDeath()) {
            if (!this.handleWardenAggro()) {
               this.updateDrinkingState();
               switch (this.phase) {
                  case IDLE:
                  case BLOCKED:
                  default:
                     break;
                  case GO_STORAGE_ANARCHY:
                     this.goStorageAnarchy();
                     break;
                  case WAIT_STORAGE_SWITCH:
                     this.waitStorageSwitch();
                     break;
                  case FIND_STASH:
                     this.findStash();
                     break;
                  case GO_STORAGE_CHEST:
                     this.goToStorageChest();
                     break;
                  case OPEN_STORAGE_CHEST:
                     this.openStorageChest();
                     break;
                  case DEPOSIT_LOOT:
                     this.depositLoot();
                     break;
                  case GO_SUPPLY_CHEST:
                     this.goToSupplyChest();
                     break;
                  case OPEN_SUPPLY_CHEST:
                     this.openSupplyChest();
                     break;
                  case TAKE_SUPPLIES:
                     this.takeSupplies();
                     break;
                  case GO_WARDEN_ANARCHY:
                     this.goWardenAnarchy();
                     break;
                  case WAIT_WARDEN_SWITCH:
                     this.waitWardenSwitch();
                     break;
                  case WAIT_HOME_TELEPORT:
                     this.waitHomeTeleport();
                     break;
                  case REFRESH_JOIN_HOLOGRAM_AWAY:
                     this.refreshJoinHologramAway();
                     break;
                  case REFRESH_JOIN_HOLOGRAM_BACK:
                     this.refreshJoinHologramBack();
                     break;
                  case PATROL_CITY:
                     this.patrolCity();
                     break;
                  case PATH_TO_SCAN_CHEST:
                     this.pathToScanChest();
                     break;
                  case INSPECT_CHEST:
                     this.inspectChest();
                     break;
                  case PREPARE_LOOT:
                     this.prepareLoot();
                     break;
                  case DARENA_COMMAND:
                     this.sendDarena();
                     break;
                  case DARENA_WAIT_GUI:
                     this.waitDarenaGui();
                     break;
                  case DARENA_CLICK:
                     this.clickDarenaSlot();
                     break;
                  case DARENA_STORAGE_COMMAND:
                     this.sendStorageDarena();
                     break;
                  case DARENA_STORAGE_WAIT_GUI:
                     this.waitStorageDarenaGui();
                     break;
                  case DARENA_STORAGE_CLICK:
                     this.clickStorageDarenaSlot();
                     break;
                  case WAIT_TIMER_HOME:
                     this.waitTimerHome();
                     break;
                  case WAIT_LOOT_TELEPORT:
                     this.waitLootTeleport();
                     break;
                  case OPEN_LOOT_CHEST:
                     this.openLootChest();
                     break;
                  case STEAL_LOOT:
                     this.stealLoot();
                     break;
                  case POST_LOOT_PATROL:
                     this.postLootPatrol();
                     break;
                  case WAIT_PVP_END_AT_CHEST:
                     this.waitPvpEndAtChest();
                     break;
                  case SET_HOME_AFTER_PVP:
                     this.setHomeAfterPvp();
                     break;
                  case RETURN_TO_STORAGE:
                     this.returnToStorage();
                     break;
                  case RECOVER_DEATH:
                     this.recoverAfterDeath();
               }
            }
         }
      }
   }

   @EventHandler
   @Native
   public void onWorldRender(WorldRenderEvent event) {
      if (this.showRoute.isValue() && !PlayerInteractionHelper.nullCheck()) {
         BlockPos target = this.getRouteRenderTarget();
         if (target != null) {
            int routeColor = this.detouring && this.detourGoal != null && this.detourGoal.equals(target) ? 16744496 : 4248319;
            Vec3d eye = mc.player.getEyePos();
            Vec3d cam = Line3D.getCameraPos();
            Vec3d center = target.toCenterPos();
            Line3D.begin(true);
            Line3D.line(
               (float)(eye.x - cam.x),
               (float)(eye.y - cam.y),
               (float)(eye.z - cam.z),
               (float)(center.x - cam.x),
               (float)(center.y - cam.y),
               (float)(center.z - cam.z),
               routeColor,
               0.86F
            );
            Line3D.box(center.x, center.y, center.z, 1.06F, routeColor, 0.92F);
            Line3D.end();
            Fill3D.begin(true);
            Fill3D.box(center.x, center.y, center.z, 1.04F, routeColor, 0.22F);
            Fill3D.end();
         }
      }
   }

   private void resetRuntime() {
      this.startedAt = System.currentTimeMillis();
      this.phaseStartedAt = this.startedAt;
      this.lastCommandAt = 0L;
      this.nextPathAt = 0L;
      this.nextOpenAt = 0L;
      this.nextRepathAt = 0L;
      this.wardenEnteredAt = 0L;
      this.targetOpenTime = 0L;
      this.storageJoinJumpStartedAt = 0L;
      this.action = "Запуск";
      this.storageChest = null;
      this.supplyChest = null;
      this.currentGoal = null;
      this.currentChest = null;
      this.lastLootedChest = null;
      this.detourGoal = null;
      this.hologramRefreshGoal = null;
      this.pvpMode = false;
      this.wasDead = false;
      this.detouring = false;
      this.lastRouteRenderSetting = this.showRoute.isValue();
      this.drinkingInvis = false;
      this.preparedWardenReturn = false;
      this.storageJoinJumpDone = false;
      this.drinkStage = AutoWarden.DrinkStage.IDLE;
      this.drinkStageStartedAt = 0L;
      this.drinkStartedAt = 0L;
      this.potionWasInventory = false;
      this.potionSourceSlot = -1;
      this.potionHotbarSlot = -1;
      this.restoreHotbarSlot = -1;
      this.cycles = 0;
      this.lootedChests = 0;
      this.valuableItemsThisTrip = 0;
      this.collectedStacks = 0;
      this.depositedStacks = 0;
      this.depositSessionStart = 0;
      this.deaths = 0;
      this.chestTimers.clear();
      this.inspectedCooldowns.clear();
      this.discoveredExtraWardenChests.clear();
      this.patrolVisitedChests.clear();
   }

   private void applyAutoWardenBaritoneSettings() {
      this.lastRouteRenderSetting = this.showRoute.isValue();
      BaritoneHelper.applySettings(true, this.lastRouteRenderSetting);
   }

   private void syncRouteRenderSetting() {
      boolean current = this.showRoute.isValue();
      if (current != this.lastRouteRenderSetting) {
         this.lastRouteRenderSetting = current;
         BaritoneHelper.applySettings(true, current);
      }
   }

   private BlockPos getRouteRenderTarget() {
      if (this.detouring && this.detourGoal != null) {
         return this.detourGoal;
      }

      if (this.currentGoal != null) {
         return this.currentGoal;
      }

      return switch (this.phase) {
         case GO_STORAGE_CHEST, OPEN_STORAGE_CHEST, DEPOSIT_LOOT -> this.storageChest;
         case GO_SUPPLY_CHEST, OPEN_SUPPLY_CHEST, TAKE_SUPPLIES -> this.supplyChest;
         default -> null;
         case REFRESH_JOIN_HOLOGRAM_AWAY -> this.hologramRefreshGoal;
         case REFRESH_JOIN_HOLOGRAM_BACK -> this.currentChest;
         case PATH_TO_SCAN_CHEST, INSPECT_CHEST, PREPARE_LOOT, DARENA_STORAGE_COMMAND, DARENA_STORAGE_WAIT_GUI, DARENA_STORAGE_CLICK, WAIT_TIMER_HOME, WAIT_LOOT_TELEPORT, OPEN_LOOT_CHEST, STEAL_LOOT, POST_LOOT_PATROL, WAIT_PVP_END_AT_CHEST, SET_HOME_AFTER_PVP -> this.currentChest;
      };
   }

   private void goStorageAnarchy() {
      this.closeHandledScreen();
      BaritoneHelper.stop();
      if (this.sendCommand("an" + this.sanitizeNumber(this.storageAnarchy.getValue(), "308"))) {
         this.storageChest = null;
         this.supplyChest = null;
         this.currentGoal = null;
         this.storageJoinJumpDone = false;
         this.storageJoinJumpStartedAt = 0L;
         this.setPhase(AutoWarden.Phase.WAIT_STORAGE_SWITCH, "Жду переход на склад");
      }
   }

   private void waitStorageSwitch() {
      if (!this.phaseElapsed((long)(this.anarchyWaitSeconds.getValue() * 1000.0F))) {
         this.action = "Жду загрузку анархии склада";
      } else {
         if (!this.storageJoinJumpDone) {
            if (this.storageJoinJumpStartedAt == 0L) {
               this.storageJoinJumpStartedAt = System.currentTimeMillis();
               if (mc.options != null) {
                  mc.options.jumpKey.setPressed(true);
               }

               this.action = "Прыгаю после захода на склад";
               return;
            }

            if (System.currentTimeMillis() - this.storageJoinJumpStartedAt < 250L) {
               if (mc.options != null) {
                  mc.options.jumpKey.setPressed(true);
               }

               this.action = "Прыгаю после захода на склад";
               return;
            }

            this.releaseStorageJoinJump();
            this.storageJoinJumpDone = true;
         }

         this.setPhase(AutoWarden.Phase.FIND_STASH, "Ищу сундуки склада");
      }
   }

   private void findStash() {
      AutoWarden.StashPair pair = this.findStashPair();
      if (pair == null) {
         BaritoneHelper.stop();
         this.setPhase(AutoWarden.Phase.BLOCKED, "Не найден склад: нужен сундук с табличкой и сундук без таблички рядом");
      } else {
         this.storageChest = pair.storageChest;
         this.supplyChest = pair.supplyChest;
         if (this.hasDepositableLoot()) {
            this.setPhase(AutoWarden.Phase.GO_STORAGE_CHEST, "Иду к сундуку хранилища");
         } else {
            this.setPhase(AutoWarden.Phase.GO_SUPPLY_CHEST, "Иду к сундуку расходников");
         }
      }
   }

   private void goToStorageChest() {
      if (this.storageChest == null) {
         this.setPhase(AutoWarden.Phase.FIND_STASH, "Повторно ищу сундук хранилища");
      } else if (this.distanceTo(this.storageChest) <= 4.5) {
         BaritoneHelper.stop();
         this.setPhase(AutoWarden.Phase.OPEN_STORAGE_CHEST, "Открываю хранилище");
      } else {
         this.ensurePathTo(this.storageChest, 2, "Иду к хранилищу");
      }
   }

   private void openStorageChest() {
      if (this.storageChest == null || this.hasSignNear(this.storageChest)) {
         this.storageChest = null;
         this.setPhase(AutoWarden.Phase.FIND_STASH, "Складовой сундук определён неверно, ищу стеш заново");
      } else if (mc.currentScreen instanceof GenericContainerScreen) {
         this.depositSessionStart = this.depositedStacks;
         this.setPhase(AutoWarden.Phase.DEPOSIT_LOOT, "Складываю лут");
      } else {
         this.retryOpenBlock(this.storageChest, "Открываю сундук хранилища");
      }
   }

   private void depositLoot() {
      if (this.storageChest != null && !this.hasSignNear(this.storageChest)) {
         if (mc.currentScreen instanceof GenericContainerScreen screen) {
            GenericContainerScreenHandler var3 = (GenericContainerScreenHandler)screen.getScreenHandler();
            if (this.depositOneLootStack(var3)) {
               this.action = "Складываю лут в хранилище";
            } else {
               this.closeHandledScreen();
               if (this.depositedStacks > this.depositSessionStart) {
                  this.cycles++;
               }

               this.valuableItemsThisTrip = 0;
               this.setPhase(AutoWarden.Phase.GO_SUPPLY_CHEST, "Проверяю расходники");
            }
         } else {
            this.setPhase(AutoWarden.Phase.OPEN_STORAGE_CHEST, "Переоткрываю хранилище");
         }
      } else {
         this.closeHandledScreen();
         this.storageChest = null;
         this.setPhase(AutoWarden.Phase.FIND_STASH, "Не складываю в сундук с табличкой, ищу склад заново");
      }
   }

   private void goToSupplyChest() {
      if (this.supplyChest == null) {
         this.setPhase(AutoWarden.Phase.FIND_STASH, "Повторно ищу сундук расходников");
      } else if (this.hasRequiredSupplies()) {
         this.setPhase(AutoWarden.Phase.GO_WARDEN_ANARCHY, "Расходники уже есть");
      } else if (this.distanceTo(this.supplyChest) <= 4.5) {
         BaritoneHelper.stop();
         this.setPhase(AutoWarden.Phase.OPEN_SUPPLY_CHEST, "Открываю расходники");
      } else {
         this.ensurePathTo(this.supplyChest, 2, "Иду к расходникам");
      }
   }

   private void openSupplyChest() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.setPhase(AutoWarden.Phase.TAKE_SUPPLIES, "Беру расходники");
      } else {
         this.retryOpenBlock(this.supplyChest, "Открываю сундук расходников");
      }
   }

   private void takeSupplies() {
      if (this.hasRequiredSupplies()) {
         this.closeHandledScreen();
         this.selectEmptyHandSlot();
         this.setPhase(AutoWarden.Phase.GO_WARDEN_ANARCHY, "Расходники собраны");
      } else if (mc.currentScreen instanceof GenericContainerScreen screen) {
         if (this.takeOneSupplyStack((GenericContainerScreenHandler)screen.getScreenHandler())) {
            this.action = "Добираю нужные расходники";
         } else {
            this.action = "Нет расходников, пополните сундук с табличкой";
            if (this.phaseElapsed(5000L)) {
               this.setPhase(AutoWarden.Phase.BLOCKED, "Нет нужных расходников в сундуке с табличкой");
            }
         }
      } else {
         this.setPhase(AutoWarden.Phase.OPEN_SUPPLY_CHEST, "Переоткрываю расходники");
      }
   }

   private void goWardenAnarchy() {
      this.closeHandledScreen();
      BaritoneHelper.stop();
      if (!this.hasRequiredSupplies()) {
         this.setPhase(AutoWarden.Phase.GO_STORAGE_ANARCHY, "Не хватает расходников, возвращаюсь на склад");
      } else {
         if (this.sendCommand("an" + this.sanitizeNumber(this.wardenAnarchy.getValue(), "110"))) {
            this.valuableItemsThisTrip = 0;
            this.currentGoal = null;
            this.patrolVisitedChests.clear();
            this.setPhase(AutoWarden.Phase.WAIT_WARDEN_SWITCH, "Жду переход на анархию вардена");
         }
      }
   }

   private void waitWardenSwitch() {
      if (!this.phaseElapsed((long)(this.anarchyWaitSeconds.getValue() * 1000.0F))) {
         this.action = "Жду загрузку анархии вардена";
      } else {
         this.wardenEnteredAt = System.currentTimeMillis();
         if (!this.hasInvisibilityEffect() && !this.drinkingInvis) {
            this.startDrinkInvis();
         }

         if (this.preparedWardenReturn && this.currentChest != null) {
            this.hologramRefreshGoal = null;
            this.setPhase(AutoWarden.Phase.REFRESH_JOIN_HOLOGRAM_AWAY, "Уже у сохранённого сундука, обновляю голограмму");
         } else {
            if (this.sendCommand("home " + this.cleanHomeName())) {
               this.setPhase(AutoWarden.Phase.WAIT_HOME_TELEPORT, "Пью невидимость и жду /home");
            }
         }
      }
   }

   private void waitHomeTeleport() {
      if (!this.phaseElapsed((long)(this.homeWaitSeconds.getValue() * 1000.0F))) {
         this.action = "Телепортируюсь на home";
      } else {
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Начинаю патруль города");
      }
   }

   private void refreshJoinHologramAway() {
      if (this.currentChest == null) {
         this.preparedWardenReturn = false;
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Сохранённый сундук потерян, выбираю новый");
      } else {
         BlockPos targetChest = this.resolveWardenChestPos(this.currentChest);
         if (this.distanceTo(targetChest) >= 7.0) {
            this.setPhase(AutoWarden.Phase.REFRESH_JOIN_HOLOGRAM_BACK, "Далеко от сундука, возвращаюсь для обновления голограммы");
         } else {
            if (this.hologramRefreshGoal == null) {
               this.hologramRefreshGoal = this.computeHologramRefreshGoal(targetChest);
            }

            if (this.hologramRefreshGoal == null) {
               this.setPhase(AutoWarden.Phase.REFRESH_JOIN_HOLOGRAM_BACK, "Не удалось выбрать точку отхода, возвращаюсь к сундуку");
            } else if (this.distanceTo(this.hologramRefreshGoal) <= 2.5) {
               BaritoneHelper.stop();
               this.currentGoal = null;
               this.hologramRefreshGoal = null;
               this.setPhase(AutoWarden.Phase.REFRESH_JOIN_HOLOGRAM_BACK, "Отошёл, возвращаюсь к сундуку");
            } else {
               this.ensurePathTo(this.hologramRefreshGoal, 2, "Отхожу от сундука, чтобы обновить голограмму");
            }
         }
      }
   }

   private void refreshJoinHologramBack() {
      if (this.currentChest == null) {
         this.preparedWardenReturn = false;
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Сохранённый сундук потерян, выбираю новый");
      } else {
         BlockPos targetChest = this.resolveWardenChestPos(this.currentChest);
         if (this.distanceTo(targetChest) > 2.5) {
            this.ensurePathTo(targetChest, 2, "Возвращаюсь к сундуку после обновления голограммы");
         } else {
            BaritoneHelper.stop();
            this.currentGoal = null;
            this.hologramRefreshGoal = null;
            this.preparedWardenReturn = false;
            this.setPhase(AutoWarden.Phase.INSPECT_CHEST, "Голограмма обновлена, проверяю сундук");
         }
      }
   }

   private void patrolCity() {
      BlockPos target = this.selectNearestSafeChest(null);
      if (target == null) {
         BaritoneHelper.stop();
         this.action = this.formatMarkedChestWaitAction();
      } else {
         this.currentChest = target;
         this.setPhase(AutoWarden.Phase.PATH_TO_SCAN_CHEST, "Иду к ближайшему сундуку");
      }
   }

   private void pathToScanChest() {
      if (this.currentChest == null) {
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Выбираю сундук для проверки");
      } else {
         BlockPos targetChest = this.resolveWardenChestPos(this.currentChest);
         if (!this.handleRouteDanger(targetChest)) {
            if (this.distanceTo(targetChest) <= 2.5) {
               BaritoneHelper.stop();
               this.setPhase(AutoWarden.Phase.INSPECT_CHEST, "Проверяю таймер сундука");
            } else {
               this.ensurePathTo(targetChest, 2, "Бегу к сундуку для проверки");
            }
         }
      }
   }

   private void inspectChest() {
      if (this.currentChest == null) {
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Выбираю следующий сундук");
      } else {
         this.markPatrolVisited(this.currentChest);
         AutoWarden.ChestTimerInfo info = this.chestTimers.get(this.currentChest);
         if (info != null && info.isLootable()) {
            this.targetOpenTime = System.currentTimeMillis();
            this.setPhase(AutoWarden.Phase.OPEN_LOOT_CHEST, "Голограммы нет, открываю сундук");
         } else if (info != null && info.getRemainingSeconds() <= this.lootTimerThreshold.getInt()) {
            this.targetOpenTime = info.timerEndTime;
            if (info.getRemainingSeconds() <= 12) {
               this.setPhase(AutoWarden.Phase.OPEN_LOOT_CHEST, "До открытия меньше 12 сек, остаюсь у сундука");
            } else {
               this.setPhase(AutoWarden.Phase.PREPARE_LOOT, "Сундук скоро откроется, сохраняю home");
            }
         } else if (info != null && this.shouldApproachMarkedChest(info)) {
            this.action = "Помеченный сундук скоро откроется: " + this.formatDuration(info.getRemainingSeconds());
         } else if (info == null && this.isWardenOpenBlocked()) {
            long left = Math.max(0L, (0L - (System.currentTimeMillis() - this.wardenEnteredAt)) / 1000L);
            this.action = "Жду серверную блокировку и голограмму: " + this.formatDuration(left);
         } else if (info == null && !this.isWardenOpenBlocked()) {
            this.targetOpenTime = System.currentTimeMillis();
            this.setPhase(AutoWarden.Phase.OPEN_LOOT_CHEST, "Голограммы нет, открываю сундук");
         } else {
            this.skipCurrentChest(info);
         }
      }
   }

   private void skipCurrentChest(AutoWarden.ChestTimerInfo info) {
      if (this.currentChest == null) {
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Выбираю следующий сундук");
      } else {
         BaritoneHelper.stop();
         this.currentGoal = null;
         this.markPatrolVisited(this.currentChest);
         this.inspectedCooldowns.put(this.currentChest, System.currentTimeMillis() + 30000L);
         String timerText = info == null ? "таймер не найден" : this.formatDuration(info.getRemainingSeconds());
         this.action = "Сундук не готов: " + timerText;
         BlockPos next = this.selectNearestSafeChest(this.currentChest);
         if (next == null) {
            this.setPhase(AutoWarden.Phase.PATROL_CITY, this.formatMarkedChestWaitAction());
         } else {
            this.currentChest = next;
            this.setPhase(AutoWarden.Phase.PATH_TO_SCAN_CHEST, "Иду к следующему сундуку");
         }
      }
   }

   private boolean shouldApproachMarkedChest(AutoWarden.ChestTimerInfo info) {
      return info != null && (info.isLootable() || info.getRemainingSeconds() <= 60);
   }

   private void prepareLoot() {
      AutoWarden.ChestTimerInfo info = this.currentChest == null ? null : this.chestTimers.get(this.currentChest);
      if (info != null && info.getRemainingSeconds() <= 12) {
         this.targetOpenTime = info.timerEndTime;
         this.setPhase(AutoWarden.Phase.OPEN_LOOT_CHEST, "До открытия меньше 12 сек, не ухожу на /darena");
      } else {
         BaritoneHelper.stop();
         if (this.sendCommand("sethome " + this.cleanHomeName())) {
            this.setPhase(AutoWarden.Phase.DARENA_COMMAND, "Home сохранён, выхожу через /darena");
         }
      }
   }

   private void sendDarena() {
      if (this.sendCommand("darena")) {
         this.setPhase(AutoWarden.Phase.DARENA_WAIT_GUI, "Жду меню выхода с арены");
      }
   }

   private void waitDarenaGui() {
      if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
         this.setPhase(AutoWarden.Phase.DARENA_CLICK, "Подтверждаю выход с арены");
      } else {
         if (this.phaseElapsed(4000L)) {
            this.setPhase(AutoWarden.Phase.WAIT_TIMER_HOME, "Меню /darena не появилось, жду таймер");
         }
      }
   }

   private void clickDarenaSlot() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
         mc.interactionManager.clickSlot(container.syncId, 24, 0, SlotActionType.PICKUP, mc.player);
      }

      this.closeHandledScreen();
      this.setPhase(AutoWarden.Phase.WAIT_TIMER_HOME, "Жду момент для /home к сундуку");
   }

   private void waitTimerHome() {
      if (this.currentChest == null) {
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Потерян целевой сундук");
      } else {
         AutoWarden.ChestTimerInfo info = this.chestTimers.get(this.currentChest);
         long remaining = info != null ? info.getRemainingSeconds() : Math.max(0L, (this.targetOpenTime - System.currentTimeMillis()) / 1000L);
         long sendHomeAt = (long)this.homeWaitSeconds.getValue() + (long)this.homeLeadSeconds.getValue();
         if (remaining > sendHomeAt) {
            this.action = "Жду открытия сундука: " + this.formatDuration(remaining);
         } else {
            if (!this.hasInvisibilityEffect() && !this.drinkingInvis) {
               this.startDrinkInvis();
            }

            if (this.sendCommand("home " + this.cleanHomeName())) {
               this.setPhase(AutoWarden.Phase.WAIT_LOOT_TELEPORT, "Телепортируюсь к сундуку перед открытием");
            }
         }
      }
   }

   private void waitLootTeleport() {
      long remaining = Math.max(0L, (this.targetOpenTime - System.currentTimeMillis()) / 1000L);
      if (!this.phaseElapsed((long)(this.homeWaitSeconds.getValue() * 1000.0F))) {
         this.action = "Жду /home, до открытия: " + this.formatDuration(remaining);
      } else {
         this.setPhase(AutoWarden.Phase.OPEN_LOOT_CHEST, "Открываю сундук вардена");
      }
   }

   private void openLootChest() {
      if (this.currentChest == null) {
         this.setPhase(AutoWarden.Phase.PATROL_CITY, "Целевой сундук потерян");
      } else if (this.isWardenOpenBlocked()) {
         long left = Math.max(0L, (0L - (System.currentTimeMillis() - this.wardenEnteredAt)) / 1000L);
         this.action = "Жду серверную блокировку сундуков: " + this.formatDuration(left);
      } else {
         AutoWarden.ChestTimerInfo info = this.chestTimers.get(this.currentChest);
         if (info != null && !info.isLootable()) {
            int remaining = info.getRemainingSeconds();
            if (info.wasHologramSeen()) {
               this.action = remaining > 0 ? "До открытия: " + this.formatDuration(remaining) : "Таймер 00:00, жду исчезновения голограммы";
               return;
            }

            if (remaining > 1) {
               this.action = "До открытия: " + this.formatDuration(remaining);
               return;
            }
         }

         if (mc.currentScreen instanceof GenericContainerScreen) {
            this.setPhase(AutoWarden.Phase.STEAL_LOOT, "Лутаю сундук");
         } else {
            BlockPos targetChest = this.resolveWardenChestPos(this.currentChest);
            if (this.distanceTo(targetChest) > 2.5) {
               this.ensurePathTo(targetChest, 2, "Подхожу к сундуку для лута");
            } else {
               this.retryOpenBlock(targetChest, "Открываю сундук вардена");
            }
         }
      }
   }

   private void stealLoot() {
      if (mc.currentScreen instanceof GenericContainerScreen screen) {
         if (this.shouldReturnToStorage()) {
            this.closeHandledScreen();
            this.finishCurrentLoot("Лимит ценных предметов набран, ухожу на склад");
         } else if (this.isInventoryFull()) {
            this.closeHandledScreen();
            this.finishCurrentLoot("Инвентарь заполнен, ухожу на склад");
         } else if (this.stealOneLootStack((GenericContainerScreenHandler)screen.getScreenHandler())) {
            this.action = "Забираю ценный предмет из сундука";
         } else {
            this.closeHandledScreen();
            this.finishCurrentLoot("В сундуке больше нет ценных предметов");
         }
      } else {
         this.setPhase(AutoWarden.Phase.OPEN_LOOT_CHEST, "Переоткрываю сундук для лута");
      }
   }

   private void finishCurrentLoot(String actionText) {
      this.lootedChests++;
      this.lastLootedChest = this.currentChest;
      this.markPatrolVisited(this.currentChest);
      if (this.currentChest != null) {
         this.inspectedCooldowns.put(this.currentChest, System.currentTimeMillis() + 30000L);
      }

      this.setPhase(AutoWarden.Phase.POST_LOOT_PATROL, actionText);
   }

   private void postLootPatrol() {
      if (this.shouldReturnToStorage()) {
         if (!this.pvpMode) {
            this.preparedWardenReturn = false;
            this.setPhase(AutoWarden.Phase.RETURN_TO_STORAGE, "Ценные предметы набраны, ухожу на склад");
         } else {
            this.moveToSafeWaitChest("Ценные предметы набраны, жду PVP-кд перед складом");
         }
      } else {
         BlockPos target = this.selectNearestSafeChest(this.lastLootedChest);
         if (target == null) {
            if (!this.pvpMode) {
               this.setPhase(AutoWarden.Phase.RETURN_TO_STORAGE, "PVP окончен, безопасных сундуков нет");
            } else {
               this.action = "Жду окончание PVP, безопасных сундуков нет";
            }
         } else {
            this.currentChest = target;
            if (this.pvpMode) {
               this.setPhase(AutoWarden.Phase.WAIT_PVP_END_AT_CHEST, "Иду к следующему сундуку, пока идёт PVP");
            } else {
               this.setPhase(AutoWarden.Phase.PATH_TO_SCAN_CHEST, "Иду к следующему сундуку");
            }
         }
      }
   }

   private void waitPvpEndAtChest() {
      if (this.currentChest == null) {
         this.setPhase(AutoWarden.Phase.POST_LOOT_PATROL, "Выбираю сундук после лута");
      } else {
         BlockPos targetChest = this.resolveWardenChestPos(this.currentChest);
         if (!this.handleRouteDanger(targetChest)) {
            if (this.distanceTo(targetChest) > 2.5) {
               this.ensurePathTo(targetChest, 2, "Иду к следующему сундуку после лута");
            } else {
               BaritoneHelper.stop();
               this.currentGoal = null;
               if (this.pvpMode) {
                  this.action = this.shouldReturnToStorage() ? "Жду окончание PVP перед переходом на склад" : "Жду окончание PVP у следующего сундука";
               } else {
                  if (this.shouldReturnToStorage()) {
                     this.preparedWardenReturn = false;
                     this.setPhase(AutoWarden.Phase.RETURN_TO_STORAGE, "PVP окончен, перехожу на склад");
                  } else {
                     this.setPhase(AutoWarden.Phase.INSPECT_CHEST, "PVP окончен, проверяю следующий сундук");
                  }
               }
            }
         }
      }
   }

   private boolean shouldReturnToStorage() {
      return this.isInventoryFull() || this.valuableItemsThisTrip >= this.valuableItemsPerTrip.getInt();
   }

   private void moveToSafeWaitChest(String actionText) {
      BlockPos target = this.selectNearestSafeChest(this.lastLootedChest);
      if (target == null) {
         this.preparedWardenReturn = false;
         if (!this.pvpMode) {
            this.setPhase(AutoWarden.Phase.RETURN_TO_STORAGE, actionText + ", безопасных сундуков нет");
         } else {
            this.action = actionText + ", безопасных сундуков нет";
         }
      } else {
         this.currentChest = target;
         this.setPhase(AutoWarden.Phase.WAIT_PVP_END_AT_CHEST, actionText);
      }
   }

   private void returnToStorage() {
      if (this.pvpMode) {
         BaritoneHelper.stop();
         this.currentGoal = null;
         this.action = "Жду окончание PVP перед переходом на склад";
      } else {
         this.setPhase(AutoWarden.Phase.GO_STORAGE_ANARCHY, "Возвращаюсь на склад");
      }
   }

   private void setHomeAfterPvp() {
      if (this.sendCommand("sethome " + this.cleanHomeName())) {
         this.preparedWardenReturn = this.currentChest != null;
         this.setPhase(AutoWarden.Phase.DARENA_STORAGE_COMMAND, "Home у следующего сундука сохранён, выхожу через /darena");
      }
   }

   private void sendStorageDarena() {
      if (this.sendCommand("darena")) {
         this.setPhase(AutoWarden.Phase.DARENA_STORAGE_WAIT_GUI, "Жду меню выхода с арены перед складом");
      }
   }

   private void waitStorageDarenaGui() {
      if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
         this.setPhase(AutoWarden.Phase.DARENA_STORAGE_CLICK, "Подтверждаю выход с арены перед складом");
      } else {
         if (this.phaseElapsed(4000L)) {
            this.setPhase(AutoWarden.Phase.RETURN_TO_STORAGE, "Меню /darena не появилось, перехожу на склад");
         }
      }
   }

   private void clickStorageDarenaSlot() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
         mc.interactionManager.clickSlot(container.syncId, 24, 0, SlotActionType.PICKUP, mc.player);
      }

      this.closeHandledScreen();
      this.setPhase(AutoWarden.Phase.RETURN_TO_STORAGE, "Вышел с арены, перехожу на склад");
   }

   private void recoverAfterDeath() {
      BaritoneHelper.stop();
      this.closeHandledScreen();
      if (this.phaseElapsed(1800L) && mc.player.isAlive()) {
         this.setPhase(AutoWarden.Phase.GO_STORAGE_ANARCHY, "После смерти иду за расходниками");
      } else {
         this.action = "Восстановление после смерти";
      }
   }

   private boolean handleDeath() {
      boolean deathScreen = mc.currentScreen instanceof DeathScreen;
      boolean dead = deathScreen || !mc.player.isAlive();
      if (!dead) {
         if (this.wasDead) {
            this.wasDead = false;
            this.setPhase(AutoWarden.Phase.GO_STORAGE_ANARCHY, "Смерть обработана, иду на склад");
         }

         return false;
      } else {
         if (!this.wasDead) {
            this.deaths++;
            this.wasDead = true;
            this.preparedWardenReturn = false;
            this.hologramRefreshGoal = null;
            this.finishDrinkingIfNeeded();
            this.releaseUseKey();
            BaritoneHelper.stop();
            this.setPhase(AutoWarden.Phase.RECOVER_DEATH, "Бот погиб, восстанавливаю цикл");
         }

         if (deathScreen && this.autoRespawn.isValue()) {
            mc.player.requestRespawn();
         }

         return true;
      }
   }

   private boolean handleWardenAggro() {
      if (!this.isEmergencyDarenaPhase() && mc.world != null && mc.player != null) {
         WardenEntity warden = this.findAggroedWarden();
         if (warden == null) {
            return false;
         }

         this.finishDrinkingIfNeeded();
         this.releaseUseKey();
         this.closeHandledScreen();
         BaritoneHelper.stop();
         this.currentGoal = null;
         this.preparedWardenReturn = false;
         this.hologramRefreshGoal = null;
         this.setPhase(AutoWarden.Phase.DARENA_STORAGE_COMMAND, "Варден заагрился, аварийно выхожу через /darena");
         return true;
      } else {
         return false;
      }
   }

   private boolean isEmergencyDarenaPhase() {
      return this.phase == AutoWarden.Phase.DARENA_COMMAND
         || this.phase == AutoWarden.Phase.DARENA_WAIT_GUI
         || this.phase == AutoWarden.Phase.DARENA_CLICK
         || this.phase == AutoWarden.Phase.DARENA_STORAGE_COMMAND
         || this.phase == AutoWarden.Phase.DARENA_STORAGE_WAIT_GUI
         || this.phase == AutoWarden.Phase.DARENA_STORAGE_CLICK
         || this.phase == AutoWarden.Phase.GO_STORAGE_ANARCHY
         || this.phase == AutoWarden.Phase.WAIT_STORAGE_SWITCH
         || this.phase == AutoWarden.Phase.FIND_STASH
         || this.phase == AutoWarden.Phase.GO_STORAGE_CHEST
         || this.phase == AutoWarden.Phase.OPEN_STORAGE_CHEST
         || this.phase == AutoWarden.Phase.DEPOSIT_LOOT
         || this.phase == AutoWarden.Phase.GO_SUPPLY_CHEST
         || this.phase == AutoWarden.Phase.OPEN_SUPPLY_CHEST
         || this.phase == AutoWarden.Phase.TAKE_SUPPLIES
         || this.phase == AutoWarden.Phase.RETURN_TO_STORAGE
         || this.phase == AutoWarden.Phase.RECOVER_DEATH
         || this.phase == AutoWarden.Phase.BLOCKED;
   }

   private WardenEntity findAggroedWarden() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof WardenEntity warden && warden.isAlive() && warden.getTarget() == mc.player) {
            return warden;
         }
      }

      return null;
   }

   private void updatePvpMode() {
      this.pvpMode = false;
      BossBarHud bossBarHud = mc.inGameHud == null ? null : mc.inGameHud.getBossBarHud();
      if (bossBarHud != null) {
         try {
            Map<UUID, ClientBossBar> bossBars = bossBarHud.bossBars;
            if (bossBars == null) {
               return;
            }

            for (ClientBossBar bossBar : bossBars.values()) {
               String name = bossBar.getName().getString().toLowerCase(Locale.ROOT);
               if (name.contains("pvp") || name.contains("пвп") || name.contains("режим боя")) {
                  this.pvpMode = true;
                  return;
               }
            }
         } catch (Throwable var6) {
         }
      }
   }

   private void scanWardenChestTimers() {
      if (mc.world != null && mc.player != null) {
         this.scanExtraWardenChestsFromHolograms();

         for (BlockPos pos : WARDEN_CHESTS) {
            if (!(this.distanceToWardenChest(pos) > 40.0)) {
               int timer = this.findTimerNearWardenChest(pos);
               AutoWarden.ChestTimerInfo info = this.chestTimers.get(pos);
               if (timer >= 0) {
                  if (info == null) {
                     this.chestTimers.put(pos, new AutoWarden.ChestTimerInfo(timer));
                  } else {
                     info.updateTimer(timer);
                  }
               } else if (info != null) {
                  if (this.isWardenOpenBlocked()) {
                     info.updateMissingDuringProtection();
                  } else {
                     info.updateMissingHologram();
                  }
               }
            }
         }
      }
   }

   private void scanExtraWardenChestsFromHolograms() {
      Box scanBox = mc.player.getBoundingBox().expand(80.0);
      boolean added = false;

      for (ArmorStandEntity entity : mc.world
         .getEntitiesByClass(
            ArmorStandEntity.class,
            scanBox,
            armorStand -> armorStand.isCustomNameVisible() && armorStand.getCustomName() != null && this.parseTimer(armorStand.getCustomName()) >= 0
         )) {
         BlockPos chestPos = this.findChestInHologramColumn(entity.getBlockPos());
         if (chestPos != null && !this.isKnownWardenChest(chestPos) && this.discoveredExtraWardenChests.add(chestPos)) {
            added = true;
         }
      }

      if (added) {
         System.out.println("AutoWarden найденные коорды: " + this.formatDiscoveredExtraWardenChests());
      }
   }

   private BlockPos findChestInHologramColumn(BlockPos hologramPos) {
      if (hologramPos == null) {
         return null;
      }

      if (this.isChestBlock(hologramPos)) {
         return hologramPos;
      }

      for (int offset = 1; offset <= 5; offset++) {
         BlockPos down = hologramPos.down(offset);
         if (this.isChestBlock(down)) {
            return down;
         }

         BlockPos up = hologramPos.up(offset);
         if (this.isChestBlock(up)) {
            return up;
         }
      }

      return null;
   }

   private boolean isKnownWardenChest(BlockPos pos) {
      if (pos == null) {
         return false;
      }

      for (BlockPos known : WARDEN_CHESTS) {
         if (known.getX() == pos.getX() && known.getZ() == pos.getZ() && Math.abs(known.getY() - pos.getY()) <= 1) {
            return true;
         }
      }

      return false;
   }

   private String formatDiscoveredExtraWardenChests() {
      return this.discoveredExtraWardenChests.isEmpty()
         ? ""
         : this.discoveredExtraWardenChests
            .stream()
            .map(pos -> pos.getX() + " " + pos.getY() + " " + pos.getZ())
            .reduce((left, right) -> left + " + " + right)
            .orElse("");
   }

   private int findTimerNearWardenChest(BlockPos basePos) {
      int bestTimer = -1;

      for (BlockPos pos : this.wardenChestVariants(basePos)) {
         int timer = this.findTimerNearBlock(pos);
         if (timer >= 0 && (bestTimer < 0 || timer < bestTimer)) {
            bestTimer = timer;
         }
      }

      return bestTimer;
   }

   private int findTimerNearBlock(BlockPos blockPos) {
      return mc.world != null && blockPos != null
         ? mc.world
            .getEntitiesByClass(ArmorStandEntity.class, new Box(blockPos).expand(3.0), entity -> entity.isCustomNameVisible() && entity.getCustomName() != null)
            .stream()
            .mapToInt(entity -> this.parseTimer(entity.getCustomName()))
            .filter(t -> t >= 0)
            .min()
            .orElse(-1)
         : -1;
   }

   private int parseTimer(Text customName) {
      if (customName == null) {
         return -1;
      }

      String raw = customName.getString();
      if (raw.isEmpty()) {
         return -1;
      }

      String cleaned = raw.replaceAll("§[0-9a-fk-or]", "")
         .replace('\r', ' ')
         .replace('\n', ' ')
         .replaceAll("[^\\p{L}\\p{N}:.]+", " ")
         .trim()
         .toLowerCase(Locale.ROOT);
      if (cleaned.isEmpty()) {
         return -1;
      }

      Matcher m = TIMER_MMSS.matcher(cleaned);
      if (m.find()) {
         int minutes = MathHelper.clamp(Integer.parseInt(m.group(1)), 0, 99);
         int seconds = MathHelper.clamp(Integer.parseInt(m.group(2)), 0, 59);
         return minutes * 60 + seconds;
      }

      m = TIMER_MINUTE_UNIT.matcher(cleaned);
      if (m.find()) {
         return MathHelper.clamp(Integer.parseInt(m.group(1)), 0, 99) * 60;
      }

      m = TIMER_SECOND_UNIT.matcher(cleaned);
      if (m.find()) {
         return MathHelper.clamp(Integer.parseInt(m.group(1)), 0, 599);
      }

      m = TIMER_NUMBER.matcher(cleaned);
      return m.find() ? MathHelper.clamp(Integer.parseInt(m.group(1)), 0, 999) : -1;
   }

   private AutoWarden.StashPair findStashPair() {
      BlockPos playerPos = mc.player.getBlockPos();
      int radius = this.stashScanRadius.getInt();
      List<BlockPos> signedChests = new ArrayList<>();
      List<BlockPos> normalChests = new ArrayList<>();

      for (int x = -radius; x <= radius; x++) {
         for (int y = -3; y <= 3; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               BlockEntity blockEntity = mc.world.getBlockEntity(pos);
               if (blockEntity != null && CHEST_TYPES.contains(blockEntity.getType())) {
                  if (this.hasSignNear(pos)) {
                     signedChests.add(pos);
                  } else {
                     normalChests.add(pos);
                  }
               }
            }
         }
      }

      signedChests.sort(Comparator.comparingDouble(this::distanceTo));
      List<BlockPos> storageCandidates = normalChests.stream()
         .filter(this::hasNormalChestNeighbor)
         .sorted(Comparator.comparingDouble(this::distanceTo))
         .toList();
      return !signedChests.isEmpty() && !storageCandidates.isEmpty() ? new AutoWarden.StashPair(storageCandidates.get(0), signedChests.get(0)) : null;
   }

   private boolean hasNormalChestNeighbor(BlockPos chestPos) {
      for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
         BlockPos neighbor = chestPos.offset(direction);
         if (this.isChestBlock(neighbor) && !this.hasSignNear(neighbor)) {
            return true;
         }
      }

      return false;
   }

   private boolean hasSignNear(BlockPos chestPos) {
      if (this.isSignBlock(chestPos.up())) {
         return true;
      }

      for (Direction direction : Direction.values()) {
         if (direction != Direction.DOWN && this.isSignBlock(chestPos.offset(direction))) {
            return true;
         }
      }

      return false;
   }

   private boolean isSignBlock(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }

      Block block = mc.world.getBlockState(pos).getBlock();
      String key = block.getTranslationKey().toLowerCase(Locale.ROOT);
      return key.contains("sign") || key.contains("таблич");
   }

   private boolean depositOneLootStack(GenericContainerScreenHandler container) {
      int containerSize = container.getInventory().size();

      for (int i = containerSize; i < container.slots.size(); i++) {
         Slot slot = (Slot)container.slots.get(i);
         ItemStack stack = slot.getStack();
         if (this.isValuableLootStack(stack)) {
            mc.interactionManager.clickSlot(container.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.depositedStacks++;
            return true;
         }
      }

      return false;
   }

   private boolean takeOneSupplyStack(GenericContainerScreenHandler container) {
      int needCarrots = this.neededCarrots();
      int needPotions = this.neededDrinkableInvisPotions();
      if (needCarrots <= 0 && needPotions <= 0) {
         return false;
      }

      Inventory inventory = container.getInventory();
      if (needCarrots > 0) {
         for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.GOLDEN_CARROT)) {
               if (stack.getCount() <= needCarrots) {
                  mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                  return true;
               }

               return this.takeExactCarrots(container, i, needCarrots);
            }
         }
      }

      if (needPotions > 0) {
         for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (this.isDrinkableInvisPotion(stack)) {
               mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
               return true;
            }
         }
      }

      return false;
   }

   private boolean takeExactCarrots(GenericContainerScreenHandler container, int sourceSlot, int amount) {
      if (amount > 0 && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         int targetSlot = this.findCarrotTargetSlot(container, amount);
         if (targetSlot == -1) {
            return false;
         }

         mc.interactionManager.clickSlot(container.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);

         for (int i = 0; i < amount; i++) {
            mc.interactionManager.clickSlot(container.syncId, targetSlot, 1, SlotActionType.PICKUP, mc.player);
         }

         mc.interactionManager.clickSlot(container.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
         return true;
      } else {
         return false;
      }
   }

   private int findCarrotTargetSlot(GenericContainerScreenHandler container, int amount) {
      int containerSize = container.getInventory().size();

      for (int i = containerSize; i < container.slots.size(); i++) {
         Slot slot = (Slot)container.slots.get(i);
         ItemStack stack = slot.getStack();
         if (stack.isOf(Items.GOLDEN_CARROT) && stack.getCount() + amount <= stack.getMaxCount()) {
            return slot.id;
         }
      }

      for (int i = containerSize; i < container.slots.size(); i++) {
         Slot slot = (Slot)container.slots.get(i);
         if (slot.getStack().isEmpty()) {
            return slot.id;
         }
      }

      return -1;
   }

   private boolean stealOneLootStack(GenericContainerScreenHandler container) {
      Inventory inventory = container.getInventory();

      for (int i = 0; i < inventory.size(); i++) {
         ItemStack stack = inventory.getStack(i);
         if (this.isValuableLootStack(stack)) {
            mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            this.collectedStacks++;
            this.valuableItemsThisTrip++;
            return true;
         }
      }

      return false;
   }

   private boolean hasDepositableLoot() {
      for (int i = 0; i < 36; i++) {
         if (this.isValuableLootStack(mc.player.getInventory().getStack(i))) {
            return true;
         }
      }

      return false;
   }

   private boolean isValuableLootStack(ItemStack stack) {
      return stack != null && !stack.isEmpty() && VALUABLE_LOOT_ITEMS.contains(stack.getItem());
   }

   private boolean hasRequiredSupplies() {
      return this.neededCarrots() <= 0 && this.neededDrinkableInvisPotions() <= 0;
   }

   private int neededCarrots() {
      return Math.max(0, this.carrotsToTake.getInt() - this.countGoldenCarrots());
   }

   private int neededDrinkableInvisPotions() {
      return this.getInvisibilitySecondsRemaining() > 180 ? 0 : Math.max(0, this.potionsToTake.getInt() - this.countDrinkableInvisPotions());
   }

   private int countGoldenCarrots() {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isOf(Items.GOLDEN_CARROT)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private int countDrinkableInvisPotions() {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         if (this.isDrinkableInvisPotion(mc.player.getInventory().getStack(i))) {
            count++;
         }
      }

      return count;
   }

   private boolean isDrinkableInvisPotion(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getItem() instanceof PotionItem && !(stack.getItem() instanceof SplashPotionItem)) {
            PotionContentsComponent contents = (PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance effect : contents.getEffects()) {
               if (effect.getEffectType() == StatusEffects.INVISIBILITY) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean startDrinkInvis() {
      if (this.drinkingInvis) {
         return true;
      }

      if (mc.player == null || mc.interactionManager == null || mc.player.currentScreenHandler == null) {
         return false;
      }

      if (this.hasInvisibilityEffect()) {
         return false;
      }

      this.restoreHotbarSlot = mc.player.getInventory().getSelectedSlot();
      this.drinkingInvis = true;
      this.drinkStage = AutoWarden.DrinkStage.FIND_POTION;
      this.drinkStageStartedAt = System.currentTimeMillis();
      this.drinkStartedAt = 0L;
      this.potionWasInventory = false;
      this.potionSourceSlot = -1;
      this.potionHotbarSlot = -1;
      this.action = "Готовлю зелье невидимости";
      if (mc.currentScreen != null) {
         this.closeHandledScreen();
      }

      return true;
   }

   private void updateDrinkingState() {
      if (this.drinkingInvis) {
         if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
            long now = System.currentTimeMillis();
            switch (this.drinkStage) {
               case IDLE:
                  this.resetDrinkState();
                  break;
               case FIND_POTION:
                  this.preparePotionForDrink(now);
                  break;
               case WAIT_SWAP:
                  this.waitPotionSwap(now);
                  break;
               case START_USE:
                  this.startUsingPotion(now);
                  break;
               case DRINKING:
                  this.continueDrinking(now);
                  break;
               case RESTORE:
                  this.restoreAfterDrink(now);
            }
         } else {
            this.resetDrinkState();
         }
      }
   }

   private void finishDrinkingIfNeeded() {
      if (!this.drinkingInvis) {
         this.resetDrinkState();
      } else {
         this.releaseUseKey();
         this.cleanupAfterDrink();
         this.resetDrinkState();
      }
   }

   private void preparePotionForDrink(long now) {
      if (mc.currentScreen != null) {
         this.closeHandledScreen();
         this.drinkStageStartedAt = now;
         this.action = "Закрываю меню перед зельем";
      } else if (now - this.drinkStageStartedAt < 120L) {
         this.action = "Готовлю зелье невидимости";
      } else if (mc.player.isUsingItem()) {
         this.action = "Жду текущий предмет перед зельем";
      } else {
         int hotbarPotionSlot = this.findHotbarSlot(this::isDrinkableInvisPotion);
         int slotId = hotbarPotionSlot >= 0 ? 36 + hotbarPotionSlot : this.findInventorySlot(this::isDrinkableInvisPotion);
         if (slotId == -1) {
            this.action = "Нет питьевого зелья невидимости";
            this.resetDrinkState();
         } else {
            this.potionSourceSlot = slotId;
            this.potionWasInventory = !this.isHotbarSlotId(slotId);
            this.potionHotbarSlot = this.potionWasInventory ? this.preferredPotionHotbarSlot() : slotId - 36;
            if (this.potionHotbarSlot < 0 || this.potionHotbarSlot > 8) {
               this.action = "Не могу выбрать слот зелья";
               this.resetDrinkState();
            } else if (this.potionWasInventory) {
               mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, this.potionSourceSlot, this.potionHotbarSlot, SlotActionType.SWAP, mc.player);
               this.selectHotbarSlot(this.potionHotbarSlot);
               this.drinkStage = AutoWarden.DrinkStage.WAIT_SWAP;
               this.drinkStageStartedAt = now;
               this.action = "Перекладываю зелье в хотбар";
            } else {
               this.selectHotbarSlot(this.potionHotbarSlot);
               this.drinkStage = AutoWarden.DrinkStage.START_USE;
               this.drinkStageStartedAt = now;
               this.action = "Выбираю зелье невидимости";
            }
         }
      }
   }

   private void waitPotionSwap(long now) {
      if (now - this.drinkStageStartedAt < 180L) {
         this.action = "Жду свап зелья";
      } else {
         if (!this.isDrinkableInvisPotion(mc.player.getInventory().getStack(this.potionHotbarSlot))) {
            int hotbarSlot = this.findHotbarSlot(this::isDrinkableInvisPotion);
            if (hotbarSlot == -1) {
               if (now - this.drinkStageStartedAt < 700L) {
                  this.action = "Жду появления зелья в хотбаре";
                  return;
               }

               this.action = "Зелье не попало в хотбар";
               this.resetDrinkState();
               return;
            }

            this.potionHotbarSlot = hotbarSlot;
         }

         this.selectHotbarSlot(this.potionHotbarSlot);
         this.drinkStage = AutoWarden.DrinkStage.START_USE;
         this.drinkStageStartedAt = now;
         this.action = "Выбираю зелье невидимости";
      }
   }

   private void startUsingPotion(long now) {
      if (now - this.drinkStageStartedAt < 80L) {
         this.action = "Жду выбор зелья";
      } else if (mc.currentScreen != null) {
         this.closeHandledScreen();
         this.drinkStageStartedAt = now;
         this.action = "Закрываю меню перед питьём";
      } else if (this.hasInvisibilityEffect()) {
         this.startRestoreAfterDrink(now);
      } else {
         if (this.potionHotbarSlot < 0 || this.potionHotbarSlot > 8 || !this.isDrinkableInvisPotion(mc.player.getInventory().getStack(this.potionHotbarSlot))) {
            int hotbarSlot = this.findHotbarSlot(this::isDrinkableInvisPotion);
            if (hotbarSlot == -1) {
               this.action = "Зелье невидимости не найдено в хотбаре";
               this.resetDrinkState();
               return;
            }

            this.potionHotbarSlot = hotbarSlot;
         }

         this.selectHotbarSlot(this.potionHotbarSlot);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         if (mc.options != null) {
            mc.options.useKey.setPressed(true);
         }

         this.drinkStage = AutoWarden.DrinkStage.DRINKING;
         this.drinkStageStartedAt = now;
         this.drinkStartedAt = now;
         this.action = "Пью невидимость";
      }
   }

   private void continueDrinking(long now) {
      this.action = "Пью невидимость";
      if (mc.options != null) {
         mc.options.useKey.setPressed(true);
      }

      if (this.hasInvisibilityEffect()) {
         this.startRestoreAfterDrink(now);
      } else if (this.drinkStartedAt <= 0L || now - this.drinkStartedAt >= 1200L) {
         if (!mc.player.isUsingItem() || now - this.drinkStartedAt >= 5000L) {
            this.startRestoreAfterDrink(now);
         }
      }
   }

   private void startRestoreAfterDrink(long now) {
      this.releaseUseKey();
      this.drinkStage = AutoWarden.DrinkStage.RESTORE;
      this.drinkStageStartedAt = now;
      this.action = "Восстанавливаю слот после зелья";
   }

   private void restoreAfterDrink(long now) {
      if (now - this.drinkStageStartedAt < 120L) {
         this.action = "Восстанавливаю слот после зелья";
      } else if (mc.currentScreen != null) {
         this.closeHandledScreen();
         this.drinkStageStartedAt = now;
         this.action = "Закрываю меню перед восстановлением слота";
      } else {
         this.cleanupAfterDrink();
         this.resetDrinkState();
      }
   }

   private void restorePotionSlot() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
         if (mc.currentScreen != null) {
            this.closeHandledScreen();
         } else {
            if (this.potionWasInventory && this.potionSourceSlot >= 0 && this.potionHotbarSlot >= 0 && this.potionHotbarSlot <= 8) {
               mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, this.potionSourceSlot, this.potionHotbarSlot, SlotActionType.SWAP, mc.player);
            }

            this.selectHotbarSlot(this.restoreHotbarSlot);
         }
      }
   }

   private void cleanupAfterDrink() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
         if (mc.currentScreen != null) {
            this.closeHandledScreen();
         } else if (!this.dropBottleFromPotionSlot() && this.isPotionStillInTemporarySlot()) {
            this.restorePotionSlot();
         } else {
            this.selectEmptyHandSlot();
         }
      }
   }

   private boolean isPotionStillInTemporarySlot() {
      return this.potionWasInventory
         && this.potionHotbarSlot >= 0
         && this.potionHotbarSlot <= 8
         && this.isDrinkableInvisPotion(mc.player.getInventory().getStack(this.potionHotbarSlot));
   }

   private boolean dropBottleFromPotionSlot() {
      if (this.potionHotbarSlot >= 0 && this.potionHotbarSlot <= 8 && mc.player != null && mc.interactionManager != null) {
         ItemStack stack = mc.player.getInventory().getStack(this.potionHotbarSlot);
         if (!stack.isOf(Items.GLASS_BOTTLE)) {
            return false;
         }

         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + this.potionHotbarSlot, 1, SlotActionType.THROW, mc.player);
         this.action = "Выкидываю пустой пузырёк";
         return true;
      } else {
         return false;
      }
   }

   private void resetDrinkState() {
      this.drinkingInvis = false;
      this.drinkStage = AutoWarden.DrinkStage.IDLE;
      this.drinkStageStartedAt = 0L;
      this.drinkStartedAt = 0L;
      this.potionWasInventory = false;
      this.potionSourceSlot = -1;
      this.potionHotbarSlot = -1;
      this.restoreHotbarSlot = -1;
   }

   private boolean hasInvisibilityEffect() {
      return this.getInvisibilityTicksRemaining() > 80;
   }

   private int getInvisibilitySecondsRemaining() {
      return this.getInvisibilityTicksRemaining() / 20;
   }

   private int getInvisibilityTicksRemaining() {
      if (mc.player == null) {
         return 0;
      }

      StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
      return effect == null ? 0 : Math.max(0, effect.getDuration());
   }

   private int findInventorySlot(AutoWarden.StackPredicate predicate) {
      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (slot.id >= 9 && slot.id <= 44 && predicate.test(slot.getStack())) {
            return slot.id;
         }
      }

      return -1;
   }

   private int preferredPotionHotbarSlot() {
      int emptySlot = this.findEmptyHotbarSlot();
      return emptySlot >= 0 ? emptySlot : this.restoreHotbarSlot;
   }

   private int findHotbarSlot(AutoWarden.StackPredicate predicate) {
      for (int i = 0; i < 9; i++) {
         if (predicate.test(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private int findEmptyHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private int findEmptyMainInventorySlotId() {
      if (mc.player != null && mc.player.currentScreenHandler != null) {
         for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.id >= 9 && slot.id <= 35 && slot.getStack().isEmpty()) {
               return slot.id;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   @Native
   private void selectEmptyHandSlot() {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
         if (mc.currentScreen != null) {
            this.closeHandledScreen();
         } else {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            if (selectedSlot >= 0 && selectedSlot <= 8 && mc.player.getInventory().getStack(selectedSlot).isEmpty()) {
               this.selectHotbarSlot(selectedSlot);
            } else {
               int emptyHotbarSlot = this.findEmptyHotbarSlot();
               if (emptyHotbarSlot >= 0) {
                  this.selectHotbarSlot(emptyHotbarSlot);
               } else {
                  if (this.moveSelectedHotbarStackToInventory()) {
                     this.selectHotbarSlot(selectedSlot);
                  }
               }
            }
         }
      }
   }

   @Native
   private boolean moveSelectedHotbarStackToInventory() {
      if (mc.player == null || mc.interactionManager == null || mc.player.currentScreenHandler == null) {
         return false;
      }

      if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         return false;
      }

      int selectedSlot = mc.player.getInventory().getSelectedSlot();
      if (selectedSlot >= 0 && selectedSlot <= 8 && !mc.player.getInventory().getStack(selectedSlot).isEmpty()) {
         int emptyInventorySlot = this.findEmptyMainInventorySlotId();
         if (emptyInventorySlot == -1) {
            return false;
         } else {
            int hotbarSlotId = 36 + selectedSlot;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hotbarSlotId, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, emptyInventorySlot, 0, SlotActionType.PICKUP, mc.player);
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hotbarSlotId, 0, SlotActionType.PICKUP, mc.player);
               return false;
            } else {
               return mc.player.getInventory().getStack(selectedSlot).isEmpty();
            }
         }
      } else {
         return false;
      }
   }

   private void selectHotbarSlot(int slot) {
      if (slot >= 0 && slot <= 8 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(slot);
         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   private boolean isInventoryFull() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void ensurePathTo(BlockPos pos, int range, String actionText) {
      if (pos != null) {
         if (!BaritoneHelper.isAvailable()) {
            this.setPhase(AutoWarden.Phase.BLOCKED, "Baritone не найден");
         } else {
            long now = System.currentTimeMillis();
            boolean needPath = this.currentGoal == null || !this.currentGoal.equals(pos) || !BaritoneHelper.isPathing();
            if (!needPath && now < this.nextPathAt) {
               this.action = actionText;
            } else if (now < this.nextPathAt && this.currentGoal != null && this.currentGoal.equals(pos)) {
               this.action = actionText;
            } else {
               if (BaritoneHelper.goTo(pos, range)) {
                  this.currentGoal = pos;
                  this.nextPathAt = now + 1200L;
                  this.action = actionText;
               }
            }
         }
      }
   }

   private boolean handleRouteDanger(BlockPos target) {
      if (target != null && System.currentTimeMillis() >= this.nextRepathAt) {
         Entity danger = this.findDangerOnRoute(target);
         if (danger == null) {
            return false;
         } else {
            BlockPos detour = this.computeDetour(danger, target);
            if (detour == null) {
               this.action = "Опасность на пути, жду";
               this.nextRepathAt = System.currentTimeMillis() + 2000L;
               BaritoneHelper.stop();
               return true;
            } else {
               BaritoneHelper.stop();
               this.currentGoal = null;
               this.detourGoal = detour;
               this.detouring = true;
               this.nextRepathAt = System.currentTimeMillis() + 2500L;
               this.ensurePathTo(this.detourGoal, 2, "Обход опасности: " + danger.getName().getString());
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private Entity findDangerOnRoute(BlockPos target) {
      Vec3d start = this.entityPos(mc.player);
      Vec3d end = target.toCenterPos();

      for (Entity entity : mc.world.getEntities()) {
         if (this.isDangerEntity(entity)) {
            double radius = this.dangerRadius(entity);
            Vec3d entityPos = this.entityPos(entity);
            if (entityPos.distanceTo(start) <= radius || entityPos.distanceTo(end) <= radius) {
               return entity;
            }

            if (this.distanceToSegment(entityPos, start, end) <= radius) {
               return entity;
            }
         }
      }

      return null;
   }

   private boolean isDangerEntity(Entity entity) {
      if (entity == null || entity == mc.player || !entity.isAlive()) {
         return false;
      } else if (entity instanceof WardenEntity) {
         return true;
      } else if (entity instanceof PlayerEntity player) {
         return player.isSpectator() ? false : !this.ignoreFriends.isValue() || !FriendUtils.isFriend(player.getName().getString());
      } else {
         return false;
      }
   }

   private double dangerRadius(Entity entity) {
      return entity instanceof WardenEntity ? this.wardenDangerRadius.getValue() : this.playerDangerRadius.getValue();
   }

   private BlockPos computeDetour(Entity danger, BlockPos target) {
      Vec3d player = this.entityPos(mc.player);
      Vec3d dangerPos = this.entityPos(danger);
      Vec3d away = player.subtract(dangerPos);
      if (away.horizontalLengthSquared() < 0.01) {
         Vec3d toTarget = target.toCenterPos().subtract(player);
         away = new Vec3d(-toTarget.z, 0.0, toTarget.x);
      }

      if (away.horizontalLengthSquared() < 0.01) {
         return null;
      }

      Vec3d normalized = new Vec3d(away.x, 0.0, away.z).normalize();
      double distance = this.dangerRadius(danger) + 8.0;
      Vec3d detour = dangerPos.add(normalized.multiply(distance));
      return BlockPos.ofFloored(detour.x, mc.player.getY(), detour.z);
   }

   private BlockPos selectNearestSafeChest(BlockPos exclude) {
      long now = System.currentTimeMillis();
      this.inspectedCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
      BlockPos markedReady = WARDEN_CHESTS.stream()
         .filter(pos -> exclude == null || !pos.equals(exclude))
         .filter(pos -> this.inspectedCooldowns.getOrDefault(pos, 0L) <= now)
         .filter(pos -> this.shouldApproachMarkedChest(this.chestTimers.get(pos)))
         .filter(pos -> !this.isUnsafeAround(this.resolveWardenChestPos(pos)))
         .min(Comparator.comparingDouble(this::distanceToWardenChest))
         .orElse(null);
      return markedReady != null
         ? markedReady
         : WARDEN_CHESTS.stream()
            .filter(pos -> exclude == null || !pos.equals(exclude))
            .filter(pos -> this.chestTimers.get(pos) == null)
            .filter(pos -> !this.patrolVisitedChests.contains(pos))
            .filter(pos -> this.inspectedCooldowns.getOrDefault(pos, 0L) <= now)
            .filter(pos -> !this.isUnsafeAround(this.resolveWardenChestPos(pos)))
            .min(Comparator.comparingDouble(this::distanceToWardenChest))
            .orElse(null);
   }

   private String formatMarkedChestWaitAction() {
      int nearestMarked = this.chestTimers
         .values()
         .stream()
         .filter(info -> !info.isLootable())
         .mapToInt(AutoWarden.ChestTimerInfo::getRemainingSeconds)
         .min()
         .orElse(-1);
      if (nearestMarked >= 0) {
         return "Жду помеченный сундук: " + this.formatDuration(nearestMarked);
      } else {
         return !this.patrolVisitedChests.isEmpty() ? "Круг патруля завершён, жду новые таймеры" : "Нет непомеченных безопасных сундуков, жду";
      }
   }

   private void markPatrolVisited(BlockPos pos) {
      if (pos != null) {
         this.patrolVisitedChests.add(pos);
      }
   }

   private BlockPos resolveWardenChestPos(BlockPos basePos) {
      if (basePos != null && mc.world != null) {
         for (BlockPos pos : this.wardenChestVariants(basePos)) {
            if (this.isChestBlock(pos)) {
               return pos;
            }
         }

         return basePos;
      } else {
         return basePos;
      }
   }

   private List<BlockPos> wardenChestVariants(BlockPos basePos) {
      if (basePos == null) {
         return List.of();
      }

      List<BlockPos> variants = new ArrayList<>(3);
      variants.add(basePos);

      for (int offset = 1; offset <= 1; offset++) {
         variants.add(basePos.down(offset));
         variants.add(basePos.up(offset));
      }

      return variants;
   }

   private boolean isChestBlock(BlockPos pos) {
      if (pos != null && mc.world != null) {
         BlockEntity blockEntity = mc.world.getBlockEntity(pos);
         return blockEntity != null && CHEST_TYPES.contains(blockEntity.getType());
      } else {
         return false;
      }
   }

   private boolean isUnsafeAround(BlockPos pos) {
      Vec3d center = pos.toCenterPos();

      for (Entity entity : mc.world.getEntities()) {
         if (this.isDangerEntity(entity) && this.entityPos(entity).distanceTo(center) <= this.dangerRadius(entity)) {
            return true;
         }
      }

      return false;
   }

   private double distanceToSegment(Vec3d point, Vec3d start, Vec3d end) {
      Vec3d line = end.subtract(start);
      double lengthSq = line.lengthSquared();
      if (lengthSq <= 1.0E-4) {
         return point.distanceTo(start);
      }

      double t = point.subtract(start).dotProduct(line) / lengthSq;
      t = Math.max(0.0, Math.min(1.0, t));
      Vec3d projection = start.add(line.multiply(t));
      return point.distanceTo(projection);
   }

   private boolean isWardenOpenBlocked() {
      return this.wardenEnteredAt > 0L && System.currentTimeMillis() - this.wardenEnteredAt < 0L;
   }

   private void retryOpenBlock(BlockPos pos, String actionText) {
      if (pos != null) {
         if (System.currentTimeMillis() < this.nextOpenAt) {
            this.action = actionText;
         } else {
            this.nextOpenAt = System.currentTimeMillis() + 700L;
            this.openBlock(pos);
            this.action = actionText;
         }
      }
   }

   private void openBlock(BlockPos pos) {
      this.stopMovementForInteraction();
      this.interactBlockDirect(this.createBlockHitResult(pos));
   }

   private void interactBlockDirect(BlockHitResult hitResult) {
      if (mc.player != null && mc.interactionManager != null) {
         this.stopMovementForInteraction();
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
         mc.player.swingHand(Hand.MAIN_HAND, false);
      }
   }

   private BlockHitResult createBlockHitResult(BlockPos pos) {
      if (mc.world != null && mc.player != null) {
         Vec3d eye = mc.player.getEyePos();
         Vec3d center = pos.toCenterPos();
         BlockHitResult rayHit = mc.world.raycast(new RaycastContext(eye, center, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
         if (rayHit.getType() == Type.BLOCK && rayHit.getBlockPos().equals(pos)) {
            return rayHit;
         }

         Direction side = this.nearestInteractSide(pos, eye);
         Vec3d hitVec = center.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
         return new BlockHitResult(hitVec, side, pos, false);
      } else {
         return new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
      }
   }

   private Direction nearestInteractSide(BlockPos pos, Vec3d eye) {
      Vec3d center = pos.toCenterPos();
      double dx = eye.x - center.x;
      double dz = eye.z - center.z;
      if (Math.abs(dx) >= Math.abs(dz) && Math.abs(dx) > 0.15) {
         return dx > 0.0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(dz) > 0.15) {
         return dz > 0.0 ? Direction.SOUTH : Direction.NORTH;
      } else {
         return eye.y >= center.y ? Direction.UP : Direction.NORTH;
      }
   }

   private void stopMovementForInteraction() {
      BaritoneHelper.stop();
      this.currentGoal = null;
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
      }

      if (mc.player != null) {
         mc.player.setSprinting(false);
         if (mc.player.input != null) {
            mc.player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
         }
      }
   }

   private boolean sendCommand(String command) {
      if (command != null && !command.isBlank() && mc.player != null && mc.player.networkHandler != null) {
         long now = System.currentTimeMillis();
         if (now - this.lastCommandAt < 850L) {
            return false;
         }

         this.lastCommandAt = now;
         String normalized = command.startsWith("/") ? command.substring(1) : command;
         mc.player.networkHandler.sendChatCommand(normalized);
         return true;
      } else {
         return false;
      }
   }

   private void closeHandledScreen() {
      if (mc.player != null && mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }
   }

   private void releaseUseKey() {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }
   }

   private void releaseStorageJoinJump() {
      if (mc.options != null) {
         mc.options.jumpKey.setPressed(false);
      }

      this.storageJoinJumpStartedAt = 0L;
   }

   private boolean phaseElapsed(long ms) {
      return System.currentTimeMillis() - this.phaseStartedAt >= ms;
   }

   private void setPhase(AutoWarden.Phase phase, String action) {
      if (this.phase != phase) {
         this.phase = phase;
         this.phaseStartedAt = System.currentTimeMillis();
         this.currentGoal = null;
      }

      this.action = action;
   }

   private double distanceToWardenChest(BlockPos basePos) {
      return basePos != null && mc.player != null
         ? this.wardenChestVariants(basePos).stream().mapToDouble(this::distanceTo).min().orElse(Double.MAX_VALUE)
         : Double.MAX_VALUE;
   }

   private double distanceTo(BlockPos pos) {
      return pos != null && mc.player != null ? this.entityPos(mc.player).distanceTo(pos.toCenterPos()) : Double.MAX_VALUE;
   }

   private BlockPos computeHologramRefreshGoal(BlockPos chestPos) {
      if (chestPos != null && mc.player != null) {
         Vec3d chestCenter = chestPos.toCenterPos();
         Vec3d away = this.entityPos(mc.player).subtract(chestCenter);
         away = new Vec3d(away.x, 0.0, away.z);
         if (away.horizontalLengthSquared() < 0.01) {
            away = new Vec3d(1.0, 0.0, 0.0);
         }

         Vec3d target = chestCenter.add(away.normalize().multiply(8.0));
         return BlockPos.ofFloored(target.x, mc.player.getY(), target.z);
      } else {
         return null;
      }
   }

   private Vec3d entityPos(Entity entity) {
      return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
   }

   private String cleanHomeName() {
      String value = this.homeName.getValue();
      return value != null && !value.isBlank() ? value.trim().replace("/", "").replace(" ", "") : "home";
   }

   private String sanitizeNumber(String value, String fallback) {
      if (value == null) {
         return fallback;
      }

      String cleaned = value.replaceAll("[^0-9]", "");
      return cleaned.isBlank() ? fallback : cleaned;
   }

   private String formatDuration(long seconds) {
      seconds = Math.max(0L, seconds);
      long minutes = seconds / 60L;
      long sec = seconds % 60L;
      return String.format(Locale.ROOT, "%02d:%02d", minutes, sec);
   }

   private boolean isHotbarSlotId(int slotId) {
      return slotId >= 36 && slotId <= 44;
   }

   private static class ChestTimerInfo {
      int timerSeconds;
      long timerEndTime;
      boolean hologramSeen;
      boolean hologramVisibleLastScan;
      boolean hologramDisappeared;
      boolean lootable;

      ChestTimerInfo(int timerSeconds) {
         this.updateTimer(timerSeconds);
      }

      void updateTimer(int timerSeconds) {
         this.timerSeconds = timerSeconds;
         this.timerEndTime = System.currentTimeMillis() + timerSeconds * 1000L;
         this.hologramSeen = true;
         this.hologramVisibleLastScan = true;
         this.hologramDisappeared = false;
         this.lootable = false;
      }

      void updateMissingHologram() {
         if (this.hologramSeen && this.getRemainingSeconds() <= 1) {
            this.hologramDisappeared = true;
         }

         this.hologramVisibleLastScan = false;
         this.lootable = this.hologramDisappeared;
      }

      void updateMissingDuringProtection() {
         this.hologramVisibleLastScan = false;
      }

      int getRemainingSeconds() {
         return (int)Math.max(0L, (this.timerEndTime - System.currentTimeMillis()) / 1000L);
      }

      boolean wasHologramSeen() {
         return this.hologramSeen;
      }

      boolean isLootable() {
         return this.hologramSeen ? this.lootable : this.getRemainingSeconds() <= 0;
      }
   }

   private enum DrinkStage {
      IDLE,
      FIND_POTION,
      WAIT_SWAP,
      START_USE,
      DRINKING,
      RESTORE;
   }

   private enum Phase {
      IDLE("Ожидание"),
      GO_STORAGE_ANARCHY("На склад через /an"),
      WAIT_STORAGE_SWITCH("Ожидание склада"),
      FIND_STASH("Поиск склада"),
      GO_STORAGE_CHEST("Путь к хранилищу"),
      OPEN_STORAGE_CHEST("Открытие хранилища"),
      DEPOSIT_LOOT("Складирование лута"),
      GO_SUPPLY_CHEST("Путь к расходникам"),
      OPEN_SUPPLY_CHEST("Открытие расходников"),
      TAKE_SUPPLIES("Забор расходников"),
      GO_WARDEN_ANARCHY("На город вардена"),
      WAIT_WARDEN_SWITCH("Ожидание города"),
      WAIT_HOME_TELEPORT("Ожидание /home"),
      REFRESH_JOIN_HOLOGRAM_AWAY("Отход от сундука"),
      REFRESH_JOIN_HOLOGRAM_BACK("Возврат к сундуку"),
      PATROL_CITY("Патруль города"),
      PATH_TO_SCAN_CHEST("Путь к сундуку"),
      INSPECT_CHEST("Проверка таймера"),
      PREPARE_LOOT("Подготовка лута"),
      DARENA_COMMAND("Выход с арены"),
      DARENA_WAIT_GUI("Ожидание меню"),
      DARENA_CLICK("Клик меню"),
      DARENA_STORAGE_COMMAND("Выход перед складом"),
      DARENA_STORAGE_WAIT_GUI("Меню выхода перед складом"),
      DARENA_STORAGE_CLICK("Клик выхода перед складом"),
      WAIT_TIMER_HOME("Ожидание открытия"),
      WAIT_LOOT_TELEPORT("Телепорт к луту"),
      OPEN_LOOT_CHEST("Открытие сундука"),
      STEAL_LOOT("Лутание"),
      POST_LOOT_PATROL("Патруль после лута"),
      WAIT_PVP_END_AT_CHEST("Ожидание PVP"),
      SET_HOME_AFTER_PVP("Сохранение home"),
      RETURN_TO_STORAGE("Возврат на склад"),
      RECOVER_DEATH("Восстановление"),
      BLOCKED("Остановлен");

      final String label;

      Phase(String label) {
         this.label = label;
      }
   }

   private interface StackPredicate {
      boolean test(ItemStack var1);
   }

   private record StashPair(BlockPos storageChest, BlockPos supplyChest) {
   }
}
