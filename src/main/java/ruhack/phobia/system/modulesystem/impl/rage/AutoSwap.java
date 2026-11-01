package ruhack.phobia.system.modulesystem.impl.rage;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ruhack.phobia.system.eventsystem.api.EventHandler;
import ruhack.phobia.system.eventsystem.impl.KeyEvent;
import ruhack.phobia.system.eventsystem.impl.TickEvent;
import ruhack.phobia.system.modulesystem.ModuleStructure;
import ruhack.phobia.system.modulesystem.category.ModuleCategory;
import ruhack.phobia.system.modulesystem.setting.implement.BindSetting;
import ruhack.phobia.system.modulesystem.setting.implement.SelectSetting;
import ruhack.phobia.system.screensystem.AutoSwapRingScreen;
import ruhack.phobia.util.player.swap.InventoryResult;
import ruhack.phobia.util.player.swap.InventoryUtils;
import ruhack.phobia.util.player.swap.SwapManager;
import ruhack.phobia.util.player.swap.SwapSettings;

public class AutoSwap extends ModuleStructure {
   private static final String SWAP_ID = "AutoSwap";
   private final SelectSetting mode = new SelectSetting(
      "Режим", "Обычный переключает два предмета, Тройной открывает колесо из трёх предметов", "Обычный", "Обычный", "Тройной"
   );
   private final BindSetting bind = new BindSetting("Кнопка свапа", "Клавиша для переключения между двумя предметами в оффханде")
      .visible(() -> this.mode.isSelected("Обычный"));
   private final BindSetting ringBind = new BindSetting("Кнопка колеса", "Клавиша для открытия колеса из трёх предметов")
      .visible(() -> this.mode.isSelected("Тройной"));
   private final SelectSetting swapMode = new SelectSetting("Режим свапа", "Быстрый - моментальный, New - остановка на один тик", "Быстрый", "Быстрый", "New")
      .visible(() -> this.mode.isSelected("Обычный"));
   private final SelectSetting firstItem = new SelectSetting("1 предмет", "Первый предмет для переключения", "Сфера", "Сфера", "Тотем")
      .visible(() -> this.mode.isSelected("Обычный"));
   private final SelectSetting secondItem = new SelectSetting("2 предмет", "Второй предмет для переключения", "Тотем", "Сфера", "Тотем")
      .visible(() -> this.mode.isSelected("Обычный"));
   private final ItemStack[] ringItems = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   private int targetSlot = -1;
   private boolean isFromHotbar = false;

   public AutoSwap() {
      super("AutoSwap", "Переключение между двумя выбранными предметами в оффханде", ModuleCategory.RAGE);
      this.settings(this.mode, this.bind, this.ringBind, this.swapMode, this.firstItem, this.secondItem);
   }

   private boolean matches(ItemStack stack, String selection) {
      if (stack != null && !stack.isEmpty()) {
         return switch (selection) {
            case "Сфера" -> stack.isOf(Items.PLAYER_HEAD);
            case "Тотем" -> stack.isOf(Items.TOTEM_OF_UNDYING);
            default -> false;
         };
      } else {
         return false;
      }
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (mc.player != null && mc.currentScreen == null) {
         this.normalizeLegacyMode();
         if (this.mode.isSelected("Тройной")) {
            if (e.isBindReleased(this.ringBind)) {
               mc.setScreen(new AutoSwapRingScreen(this, mc.currentScreen));
            }
         } else if (e.isBindReleased(this.bind)) {
            this.swapTo(this.resolveToggleTarget());
         }
      }
   }

   public String getRingItemName(int slot) {
      ItemStack stack = this.getRingItemStack(slot);
      return stack.isEmpty() ? "Выбрать предмет" : stack.getName().getString();
   }

   public ItemStack getRingItemStack(int slot) {
      return this.isRingSlot(slot) ? this.ringItems[slot] : ItemStack.EMPTY;
   }

   public boolean isSelectableRingItem(ItemStack stack) {
      return stack != null && !stack.isEmpty() && (stack.isOf(Items.PLAYER_HEAD) || stack.isOf(Items.TOTEM_OF_UNDYING));
   }

   public void selectRingItem(int slot, ItemStack stack) {
      if (this.isRingSlot(slot) && this.isSelectableRingItem(stack)) {
         this.ringItems[slot] = stack.copyWithCount(1);
      }
   }

   public void clearRingItem(int slot) {
      if (this.isRingSlot(slot)) {
         this.ringItems[slot] = ItemStack.EMPTY;
      }
   }

   public void validateRingItems() {
      for (int slot = 0; slot < this.ringItems.length; slot++) {
         if (!this.ringItems[slot].isEmpty()
            && this.findExactItem(this.ringItems[slot]) == -1
            && !this.sameRingItem(mc.player == null ? ItemStack.EMPTY : mc.player.getOffHandStack(), this.ringItems[slot])) {
            this.ringItems[slot] = ItemStack.EMPTY;
         }
      }
   }

   public boolean hasRingItem(int slot) {
      return this.isRingSlot(slot) && mc.player != null && !this.ringItems[slot].isEmpty()
         ? this.sameRingItem(mc.player.getOffHandStack(), this.ringItems[slot]) || this.findExactItem(this.ringItems[slot]) >= 0
         : false;
   }

   public boolean isRingItemEquipped(int slot) {
      return this.isRingSlot(slot) && mc.player != null && this.sameRingItem(mc.player.getOffHandStack(), this.ringItems[slot]);
   }

   public boolean swapRingItem(int slot) {
      if (this.isRingSlot(slot) && mc.player != null && !this.ringItems[slot].isEmpty() && !SwapManager.isSwapQueued("AutoSwap")) {
         ItemStack selected = this.ringItems[slot];
         if (this.sameRingItem(mc.player.getOffHandStack(), selected)) {
            return false;
         } else {
            int inventorySlot = this.findExactItem(selected);
            if (inventorySlot < 0) {
               this.clearRingItem(slot);
               return false;
            } else {
               this.prepareSwapNew(inventorySlot);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void swapTo(String targetItem) {
      if (this.swapMode.isSelected("Быстрый")) {
         this.executeSwapInstant(targetItem);
      } else if (!SwapManager.isSwapQueued("AutoSwap")) {
         this.prepareSwapNew(targetItem);
      }
   }

   private String resolveToggleTarget() {
      String item1 = this.firstItem.getValue();
      String item2 = this.secondItem.getValue();
      ItemStack currentOffhand = mc.player.getOffHandStack();
      if (this.matches(currentOffhand, item1)) {
         return item2;
      } else {
         return this.matches(currentOffhand, item2) ? item1 : item1;
      }
   }

   private void executeSwapInstant(String targetItem) {
      if (!this.matches(mc.player.getOffHandStack(), targetItem)) {
         InventoryResult hotbar = this.findItemHotbar(targetItem);
         if (hotbar.found()) {
            int guiSlot = hotbar.slot() + 36;
            InventoryUtils.swapToOffhand(guiSlot);
         } else {
            InventoryResult inv = this.findItemInventory(targetItem);
            if (inv.found()) {
               InventoryUtils.swapToOffhand(inv.slot());
            }
         }
      }
   }

   private void prepareSwapNew(String targetItem) {
      if (!this.matches(mc.player.getOffHandStack(), targetItem)) {
         InventoryResult hotbar = this.findItemHotbar(targetItem);
         if (hotbar.found()) {
            this.targetSlot = hotbar.slot();
            this.isFromHotbar = true;
         } else {
            InventoryResult inv = this.findItemInventory(targetItem);
            if (!inv.found()) {
               return;
            }

            this.targetSlot = inv.slot();
            this.isFromHotbar = false;
         }

         SwapManager.queueSwap("AutoSwap", 30, this::executeSwapLegit, this.createOneTickSettings(), this::cleanup);
      }
   }

   private void prepareSwapNew(int inventorySlot) {
      this.targetSlot = inventorySlot;
      this.isFromHotbar = inventorySlot < 9;
      SwapManager.queueSwap("AutoSwap", 30, this::executeSwapLegit, this.createOneTickSettings(), this::cleanup);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.normalizeLegacyMode();
      if (mc.player != null) {
         SwapManager.tick();
      }
   }

   private void normalizeLegacyMode() {
      if (!this.mode.getList().contains(this.mode.getValue())) {
         if ("New".equals(this.mode.getValue())) {
            this.swapMode.setValue("New");
         }

         this.mode.setValue("Обычный");
      }
   }

   private void executeSwapLegit() {
      if (this.isFromHotbar) {
         int guiSlot = this.targetSlot + 36;
         InventoryUtils.swapToOffhand(guiSlot);
      } else {
         InventoryUtils.swapToOffhand(this.targetSlot);
      }
   }

   private void cleanup() {
      this.targetSlot = -1;
      this.isFromHotbar = false;
   }

   private SwapSettings createOneTickSettings() {
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

   private InventoryResult findItemHotbar(String selection) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (this.matches(stack, selection)) {
            return new InventoryResult(i, true, stack);
         }
      }

      return InventoryResult.notFound();
   }

   private InventoryResult findItemInventory(String selection) {
      if (mc.player == null) {
         return InventoryResult.notFound();
      }

      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (this.matches(stack, selection)) {
            return InventoryResult.of(i, stack);
         }
      }

      return InventoryResult.notFound();
   }

   private int findExactItem(ItemStack selected) {
      if (mc.player != null && selected != null && !selected.isEmpty()) {
         for (int slot = 0; slot < 36; slot++) {
            if (this.sameRingItem(mc.player.getInventory().getStack(slot), selected)) {
               return slot;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean sameRingItem(ItemStack first, ItemStack second) {
      return this.isSelectableRingItem(first) && this.isSelectableRingItem(second) && ItemStack.areItemsAndComponentsEqual(first, second);
   }

   private boolean isRingSlot(int slot) {
      return slot >= 0 && slot < this.ringItems.length;
   }

   @Override
   public void deactivate() {
      SwapManager.cancelSwap("AutoSwap");
      this.cleanup();
   }
}
