package ruhack.phobia.system.modulesystem.impl.rage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BooleanSetting;
import ruhack.phobia.system.modulesystem.setting.implement.MultiSelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SliderSettings;
import ruhack.phobia.util.player.swap.InventoryResult;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.SwapManager;
import ruhack.phobia.util.player.swap.SwapSettings;

public class AutoTotem extends ModuleStructure {
   private static final int OFFHAND_SLOT = 45;
   private static final String SWAP_ID = "AutoTotem";
   private static final long SWAP_COOLDOWN = 500L;
   private static final String CHECK_ELYTRA = "Здоровье на элитрах";
   private static final String CHECK_CRYSTAL = "Кристалл";
   private static final String CHECK_GOLDEN_HEARTS = "Золотые сердца";
   private static final String CHECK_TNT = "Динамит";
   private static final String CHECK_TNT_MINECART = "Вагонетка с динамитом";
   private static final String CHECK_ARMOR = "Не полная броня";
   private final SelectSetting mode = new SelectSetting("Режим", "Быстрый - моментальный, New - остановка на один тик", "Быстрый", "Быстрый", "New");
   private final SliderSettings healthThreshold = new SliderSettings("Порог ХП", "ХП, ниже которого ставится тотем", 6.0F).range(1.0F, 20.0F).step(0.5F);
   private final MultiSelectSetting checks = new MultiSelectSetting("Чеки", "Условия автоматической установки тотема")
      .value("Здоровье на элитрах", "Кристалл", "Золотые сердца", "Динамит", "Вагонетка с динамитом", "Не полная броня")
      .selected("Здоровье на элитрах", "Кристалл");
   private final SliderSettings elytraHealth = new SliderSettings("ХП на элитрах", "Порог здоровья при полёте на элитрах", 10.0F)
      .range(1.0F, 20.0F)
      .step(0.5F)
      .visible(() -> this.check("Здоровье на элитрах"));
   private final SliderSettings crystalDistance = new SliderSettings("Дистанция кристалла", "Максимальная дистанция до кристалла", 6.0F)
      .range(1.0F, 12.0F)
      .step(0.5F)
      .visible(() -> this.check("Кристалл"));
   private final BooleanSetting noSwapIfBall = new BooleanSetting("Не брать если шар", "Не брать тотем на кристалл если шар в оффханде")
      .setValue(true)
      .visible(() -> this.check("Кристалл"));
   private final BooleanSetting lowHpOverride = new BooleanSetting("Брать если мало ХП", "Игнорировать 'Не брать если шар' при критическом ХП")
      .setValue(false)
      .visible(() -> this.check("Кристалл") && this.noSwapIfBall.isValue());
   private final SliderSettings crystalHealth = new SliderSettings("ХП на кристалл", "Порог здоровья для игнорирования шара", 4.0F)
      .range(1.0F, 20.0F)
      .step(0.5F)
      .visible(() -> this.check("Кристалл") && this.noSwapIfBall.isValue() && this.lowHpOverride.isValue());
   private final BooleanSetting fallCheck = new BooleanSetting("Падение", "Брать тотем при падении с высоты").setValue(true);
   private final SliderSettings fallHeight = new SliderSettings("Высота падения", "Минимальная высота падения для тотема", 15.0F)
      .range(3.0F, 50.0F)
      .step(1.0F)
      .visible(this.fallCheck::isValue);
   private final SliderSettings armorBonusHealth = new SliderSettings("Бонус ХП за броню", "Сколько ХП добавить к порогу при неполной броне", 2.0F)
      .range(0.5F, 10.0F)
      .step(0.5F)
      .visible(() -> this.check("Не полная броня"));
   private final BooleanSetting revertItem = new BooleanSetting("Возвращение предмета", "Вернуть предмет после восстановления ХП").setValue(true);
   private final BooleanSetting saveTalismans = new BooleanSetting("Сохранение талисманов", "Приоритет обычных тотемов над зачарованными").setValue(true);
   private final SliderSettings tntDistance = new SliderSettings("Дистанция динамита", "Максимальная дистанция до динамита", 5.0F)
      .range(1.0F, 10.0F)
      .step(0.5F)
      .visible(() -> this.check("Динамит"));
   private final SliderSettings tntMinecartDistance = new SliderSettings("Дистанция вагонетки", "Максимальная дистанция до вагонетки с TNT", 5.0F)
      .range(1.0F, 10.0F)
      .step(0.5F)
      .visible(() -> this.check("Вагонетка с динамитом"));
   private Deque<ItemStack> itemHistory = new ArrayDeque<>();
   private int totemSlot = -1;
   private long lastSwapTime = 0L;
   private boolean hasReplacedEnchanted = false;

   public AutoTotem() {
      super("AutoTotem", "Автоматически ставит тотем в оффханд при опасности", ModuleCategory.RAGE);
      this.settings(
         this.mode,
         this.healthThreshold,
         this.checks,
         this.elytraHealth,
         this.crystalDistance,
         this.noSwapIfBall,
         this.lowHpOverride,
         this.crystalHealth,
         this.fallCheck,
         this.fallHeight,
         this.armorBonusHealth,
         this.revertItem,
         this.saveTalismans,
         this.tntDistance,
         this.tntMinecartDistance
      );
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         SwapManager.tick();
         if (!SwapManager.isSwapQueued("AutoTotem")) {
            if (System.currentTimeMillis() - this.lastSwapTime >= 500L) {
               if (this.needsTotem()) {
                  boolean isCritical = this.getEffectiveHealth() <= this.getEffectiveThreshold();
                  if (!this.hasTotemInOffhand()) {
                     this.hasReplacedEnchanted = false;
                     this.startTotemPlacement(isCritical || !this.saveTalismans.isValue());
                  } else if (this.shouldReplaceEnchantedTotem()) {
                     this.startTotemPlacement(false);
                  }
               } else {
                  this.hasReplacedEnchanted = false;
                  if (this.canRevert()) {
                     this.startRevert();
                  }
               }
            }
         }
      }
   }

   private boolean needsTotem() {
      float health = this.getEffectiveHealth();
      float threshold = this.getEffectiveThreshold();
      if (health <= threshold) {
         return true;
      }

      if (this.check("Здоровье на элитрах") && mc.player.isGliding() && health <= this.elytraHealth.getValue()) {
         return true;
      }

      if (this.check("Кристалл") && this.hasCrystalNearby()) {
         boolean hasBallInOffhand = this.hasPlayerHeadInOffhand();
         if (!this.noSwapIfBall.isValue() || !hasBallInOffhand) {
            return true;
         } else {
            return this.lowHpOverride.isValue() ? health <= this.crystalHealth.getValue() : false;
         }
      } else if (this.fallCheck.isValue() && mc.player.fallDistance >= this.fallHeight.getValue()) {
         return true;
      } else {
         return this.check("Динамит") && this.hasTntNearby() ? true : this.check("Вагонетка с динамитом") && this.hasTntMinecartNearby();
      }
   }

   private boolean check(String name) {
      return this.checks.isSelected(name);
   }

   private float getEffectiveHealth() {
      float health = mc.player.getHealth();
      if (this.check("Золотые сердца")) {
         health += mc.player.getAbsorptionAmount();
      }

      return health;
   }

   private float getEffectiveThreshold() {
      float threshold = this.healthThreshold.getValue();
      if (this.check("Не полная броня")) {
         int armorPoints = this.getArmorPoints();
         if (armorPoints < 20) {
            threshold += this.armorBonusHealth.getValue();
         }
      }

      return threshold;
   }

   private int getArmorPoints() {
      if (mc.player == null) {
         return 0;
      }

      int points = 0;
      ItemStack helmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
      ItemStack chestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      ItemStack leggings = mc.player.getEquippedStack(EquipmentSlot.LEGS);
      ItemStack boots = mc.player.getEquippedStack(EquipmentSlot.FEET);
      points += this.getArmorValue(helmet);
      points += this.getArmorValue(chestplate);
      points += this.getArmorValue(leggings);
      return points + this.getArmorValue(boots);
   }

   private int getArmorValue(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      } else if (stack.getItem() == Items.NETHERITE_HELMET) {
         return 3;
      } else if (stack.getItem() == Items.NETHERITE_CHESTPLATE) {
         return 8;
      } else if (stack.getItem() == Items.NETHERITE_LEGGINGS) {
         return 6;
      } else if (stack.getItem() == Items.NETHERITE_BOOTS) {
         return 3;
      } else if (stack.getItem() == Items.DIAMOND_HELMET) {
         return 3;
      } else if (stack.getItem() == Items.DIAMOND_CHESTPLATE) {
         return 8;
      } else if (stack.getItem() == Items.DIAMOND_LEGGINGS) {
         return 6;
      } else if (stack.getItem() == Items.DIAMOND_BOOTS) {
         return 3;
      } else if (stack.getItem() == Items.IRON_HELMET) {
         return 2;
      } else if (stack.getItem() == Items.IRON_CHESTPLATE) {
         return 6;
      } else if (stack.getItem() == Items.IRON_LEGGINGS) {
         return 5;
      } else if (stack.getItem() == Items.IRON_BOOTS) {
         return 2;
      } else {
         return stack.getItem() == Items.GOLDEN_HELMET ? 3 : 1;
      }
   }

   private boolean hasImmediateThreat() {
      if (this.check("Кристалл") && this.hasCrystalNearby()) {
         return true;
      } else if (this.check("Динамит") && this.hasTntNearby()) {
         return true;
      } else {
         return this.check("Вагонетка с динамитом") && this.hasTntMinecartNearby()
            ? true
            : this.fallCheck.isValue() && mc.player != null && mc.player.fallDistance >= this.fallHeight.getValue();
      }
   }

   private boolean hasCrystalNearby() {
      return this.hasEntityNearby(entity -> entity instanceof EndCrystalEntity, this.crystalDistance.getValue());
   }

   private boolean hasTntNearby() {
      return this.hasEntityNearby(entity -> entity instanceof TntEntity, this.tntDistance.getValue());
   }

   private boolean hasTntMinecartNearby() {
      return this.hasEntityNearby(entity -> entity instanceof TntMinecartEntity, this.tntMinecartDistance.getValue());
   }

   private boolean hasEntityNearby(Predicate<Entity> predicate, double maxDistance) {
      if (mc.world != null && mc.player != null) {
         Box searchBox = mc.player.getBoundingBox().expand(maxDistance);

         for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (predicate.test(entity) && mc.player.distanceTo(entity) <= maxDistance) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean hasTotemInOffhand() {
      return mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
   }

   private boolean hasPlayerHeadInOffhand() {
      return mc.player.getOffHandStack().getItem() == Items.PLAYER_HEAD;
   }

   private boolean shouldReplaceEnchantedTotem() {
      if (!this.saveTalismans.isValue()) {
         return false;
      } else if (this.hasReplacedEnchanted) {
         return false;
      } else {
         ItemStack offhand = mc.player.getOffHandStack();
         if (offhand.getItem() != Items.TOTEM_OF_UNDYING) {
            return false;
         } else if (!this.hasSpecialProperties(offhand)) {
            return false;
         } else {
            InventoryResult normalTotem = this.findTotem(false);
            if (normalTotem.found()) {
               this.hasReplacedEnchanted = true;
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private boolean hasSpecialProperties(ItemStack stack) {
      if (stack.hasEnchantments()) {
         return true;
      } else if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
         return true;
      } else {
         return stack.contains(DataComponentTypes.LORE) ? true : stack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      }
   }

   private InventoryResult findTotem(boolean allowEnchanted) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      }

      if (this.saveTalismans.isValue() && !allowEnchanted) {
         for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isValidTotem(stack, false)) {
               return InventoryResult.of(i, stack);
            }
         }

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isValidTotem(stack, false)) {
               return InventoryResult.of(i + 36, stack);
            }
         }
      }

      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (this.isValidTotem(stack, true)) {
            return InventoryResult.of(i, stack);
         }
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (this.isValidTotem(stack, true)) {
            return InventoryResult.of(i + 36, stack);
         }
      }

      return InventoryResult.notFound();
   }

   private boolean isValidTotem(ItemStack stack, boolean allowEnchanted) {
      return stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING ? false : allowEnchanted || !this.hasSpecialProperties(stack);
   }

   private void startTotemPlacement(boolean allowEnchanted) {
      InventoryResult totemResult = this.findTotem(false);
      if (!totemResult.found() && allowEnchanted) {
         totemResult = this.findTotem(true);
      }

      if (totemResult.found()) {
         if (this.revertItem.isValue() && this.itemHistory.isEmpty()) {
            ItemStack currentOffhand = mc.player.getOffHandStack();
            if (!currentOffhand.isEmpty() && (currentOffhand.getItem() != Items.TOTEM_OF_UNDYING || this.hasSpecialProperties(currentOffhand))) {
               this.itemHistory.push(currentOffhand.copy());
            }
         }

         this.totemSlot = totemResult.slot();
         String swapMode = this.mode.getValue();
         if (!swapMode.equals("Быстрый")) {
            SwapManager.queueSwap("AutoTotem", 0, this::executeTotemSwap, this.createSwapSettings(), this::cleanupAfterSwap);
         } else {
            this.executeTotemSwap();
            this.cleanupAfterSwap();
         }
      }
   }

   private void executeTotemSwap() {
      if (this.totemSlot != -1) {
         InventoryUtils.swapToOffhand(this.totemSlot);
      }
   }

   private boolean canRevert() {
      if (!this.revertItem.isValue()) {
         return false;
      }

      if (this.itemHistory.isEmpty()) {
         return false;
      }

      ItemStack offhand = mc.player.getOffHandStack();
      boolean offhandIsTotemOrEmpty = offhand.isEmpty() || offhand.getItem() == Items.TOTEM_OF_UNDYING;
      if (!offhandIsTotemOrEmpty) {
         return false;
      }

      float health = this.getEffectiveHealth();
      float baseThreshold = this.healthThreshold.getValue();
      float safetyBuffer = 1.0F;
      return health <= baseThreshold + safetyBuffer ? false : !this.hasImmediateThreat();
   }

   private void startRevert() {
      InventoryResult previousResult = this.findPreviousItem();
      if (previousResult.found()) {
         this.totemSlot = previousResult.slot();
         String swapMode = this.mode.getValue();
         if (!swapMode.equals("Быстрый")) {
            SwapManager.queueSwap("AutoTotem", 30, this::executeRevertSwap, this.createSwapSettings(), this::cleanupAfterRevert);
         } else {
            this.executeRevertSwap();
            this.cleanupAfterRevert();
         }
      }
   }

   private InventoryResult findPreviousItem() {
      if (mc.player != null && !this.itemHistory.isEmpty()) {
         ItemStack targetItem = this.itemHistory.peek();

         for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, targetItem)) {
               return InventoryResult.of(i, stack);
            }
         }

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, targetItem)) {
               return InventoryResult.of(i + 36, stack);
            }
         }

         return InventoryResult.notFound();
      } else {
         return InventoryResult.notFound();
      }
   }

   private void executeRevertSwap() {
      if (this.totemSlot != -1) {
         InventoryUtils.swapToOffhand(this.totemSlot);
      }
   }

   private SwapSettings createSwapSettings() {
      return new SwapSettings()
         .stopMovement(true)
         .stopSprint(false)
         .closeInventory(false)
         .preStopDelay(0, 0)
         .waitStopDelay(0, 0)
         .minimumStopDelay(50)
         .preSwapDelay(0, 0)
         .postSwapDelay(0, 0)
         .resumeDelay(0, 0)
         .velocityThreshold(0.003);
   }

   private void cleanupAfterSwap() {
      this.totemSlot = -1;
      this.lastSwapTime = System.currentTimeMillis();
   }

   private void cleanupAfterRevert() {
      this.totemSlot = -1;
      if (!this.itemHistory.isEmpty()) {
         this.itemHistory.pop();
      }

      this.lastSwapTime = System.currentTimeMillis();
   }

   @Override
   public void deactivate() {
      SwapManager.cancelSwap("AutoTotem");
      this.totemSlot = -1;
      this.itemHistory.clear();
   }
}
